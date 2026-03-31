import type { FastifyPluginAsync } from 'fastify';
import { eq, sql } from 'drizzle-orm';
import { db } from '../drizzle/db.js';
import { component, requestItem } from '../drizzle/schema.js';
import type { InferSelectModel } from 'drizzle-orm';
import { requireAuth, requireAdminOrTA } from '../middleware/auth.js';
import {
  UserRole,
  categoryValues,
  locationValues,
  toLocationEnum,
  AuditActionType,
} from '../utils/enums.js';
import type { CategoryValue, LocationValue, UserRoleValue } from '../utils/enums.js';
import { logAudit, getUserIdFromRequest } from '../utils/audit.js';
import { isValidHttpUrl } from '../utils/validation.js';

const componentBodySchema = {
  type: 'object',
  properties: {
    name: { type: 'string', minLength: 1, maxLength: 200 },
    description: { type: 'string', maxLength: 2000 },
    imageUrl: { type: 'string', maxLength: 2000 },
    totalQuantity: { type: 'integer', minimum: 0 },
    availableQuantity: { type: 'integer', minimum: 0 },
    category: { type: 'string', maxLength: 100 },
    location: { type: 'string', maxLength: 100 },
  },
  additionalProperties: false,
} as const;

const createComponentSchema = {
  body: { ...componentBodySchema, required: ['name'] as ['name'] },
} as const;

const updateComponentSchema = {
  body: componentBodySchema,
} as const;

const componentsRoutes: FastifyPluginAsync = async (app) => {
  app.get('/', { preHandler: requireAuth }, async (request, reply) => {
    try {
      const components = await db.query.component.findMany({
        orderBy: (components, { desc }) => [desc(components.createdAt)],
      });
      if (components.length === 0) return reply.code(204).send();

      // Find latest updatedAt/createdAt using string comparison (ISO format sorts lexicographically)
      let latestDateStr = '';
      for (const c of components) {
        const dateStr = (c.updatedAt ?? c.createdAt) as string;
        if (dateStr > latestDateStr) {
          latestDateStr = dateStr;
        }
      }

      if (!latestDateStr) {
        return reply.send({ components });
      }

      const isoStr = latestDateStr.includes('T') ? latestDateStr : latestDateStr.replace(' ', 'T');
      const lastModifiedDate = new Date(isoStr.endsWith('Z') ? isoStr : isoStr + 'Z');
      const lastModifiedHeader = lastModifiedDate.toUTCString();

      const modifiedSinceHeader = request.headers['if-modified-since'];
      if (modifiedSinceHeader) {
        const clientDate = new Date(modifiedSinceHeader);
        const clientIso = clientDate.toISOString();
        const serverSec = isoStr.substring(0, 19);
        const clientSec = clientIso.substring(0, 19);

        if (clientSec >= serverSec) {
          return reply.code(304).send();
        }
      }
      return reply
        .headers({
          'Last-Modified': lastModifiedHeader,
          'Cache-Control': 'private, max-age=0, must-revalidate',
        })
        .send({
          components,
          lastModified: lastModifiedHeader,
        });
    } catch (err) {
      app.log.error(err);
      return reply.code(500).send({ error: 'failed to fetch components' });
    }
  });

  app.get('/export/csv', { preHandler: requireAuth }, async (request, reply) => {
    const userRole = (request.user as { role?: UserRoleValue })?.role;
    if (userRole !== UserRole.ADMIN && userRole !== UserRole.TA && userRole !== UserRole.FACULTY) {
      return reply.code(403).send({ error: 'forbidden: admin, TA, or faculty role required' });
    }

    try {
      const components = await db.query.component.findMany({
        orderBy: (components, { desc }) => [desc(components.createdAt)],
      });

      const csvHeader = 'Name,Description,Category,Location,Total Quantity,Available Quantity';
      const csvRows = components.map((c: InferSelectModel<typeof component>) => {
        const escapeCsv = (value: string | null | undefined) => {
          if (value == null) return '';
          const str = value.replace(/"/g, '""');
          return str.includes(',') || str.includes('"') || str.includes('\n') ? `"${str}"` : str;
        };
        return [
          escapeCsv(c.name),
          escapeCsv(c.description),
          escapeCsv(c.category?.replace(/_/g, ' ') ?? null),
          escapeCsv(c.location?.replace(/_/g, ' ') ?? null),
          c.totalQuantity,
          c.availableQuantity,
        ].join(',');
      });

      const csv = [csvHeader, ...csvRows].join('\n');

      return reply
        .header('Content-Type', 'text/csv')
        .header('Content-Disposition', 'attachment; filename="components.csv"')
        .send(csv);
    } catch (err) {
      app.log.error(err);
      return reply.code(500).send({ error: 'failed to export components' });
    }
  });

  app.get('/:id', { preHandler: requireAuth }, async (request, reply) => {
    const params = request.params as { id?: string };
    const id = params?.id;

    if (!id) {
      return reply.code(400).send({ error: 'component id is required' });
    }

    try {
      const found = await db.query.component.findFirst({
        where: (component, { eq }) => eq(component.id, id),
      });

      if (!found) {
        return reply.code(404).send({ error: 'component not found' });
      }

      return reply.send({ component: found });
    } catch (err) {
      app.log.error(err);
      return reply.code(500).send({ error: 'failed to fetch component' });
    }
  });

  app.post(
    '/',
    { preHandler: requireAdminOrTA, schema: createComponentSchema },
    async (request, reply) => {
      const body = request.body as {
        name?: string;
        description?: string;
        imageUrl?: string;
        totalQuantity?: number;
        availableQuantity?: number;
        category?: string;
        location?: string;
      };

      const name = body?.name?.trim();
      const description = body?.description?.trim();
      const imageUrl = body?.imageUrl?.trim();
      const totalQuantity = body?.totalQuantity;
      const availableQuantity = body?.availableQuantity;
      const category = body?.category?.trim();
      const location = body?.location?.trim();

      if (!name) {
        return reply.code(400).send({ error: 'name is required' });
      }

      if (totalQuantity !== undefined && (typeof totalQuantity !== 'number' || totalQuantity < 0)) {
        return reply.code(400).send({ error: 'totalQuantity must be a non-negative number' });
      }

      if (
        availableQuantity !== undefined &&
        (typeof availableQuantity !== 'number' || availableQuantity < 0)
      ) {
        return reply.code(400).send({ error: 'availableQuantity must be a non-negative number' });
      }

      if (category && !categoryValues.includes(category as CategoryValue)) {
        return reply.code(400).send({
          error: `invalid category. Must be one of ${categoryValues.join(', ')} (use Others if none apply)`,
        });
      }

      if (location && !locationValues.includes(location as LocationValue)) {
        return reply
          .code(400)
          .send({ error: `invalid location. Must be one of ${locationValues.join(', ')}` });
      }

      if (imageUrl && !isValidHttpUrl(imageUrl)) {
        return reply.code(400).send({ error: 'imageUrl must be a valid HTTP or HTTPS URL' });
      }

      try {
        const now = new Date().toISOString();
        const [created] = await db
          .insert(component)
          .values({
            id: crypto.randomUUID(),
            name,
            description: description || null,
            imageUrl: imageUrl || null,
            totalQuantity: totalQuantity ?? 0,
            availableQuantity: availableQuantity ?? totalQuantity ?? 0,
            category: category ? (category as CategoryValue) : null,
            location: location ? toLocationEnum(location) : null,
            createdAt: now,
            updatedAt: now,
          })
          .returning();

        if (!created) {
          return reply.code(500).send({ error: 'failed to create component' });
        }

        await logAudit(
          {
            userId: getUserIdFromRequest(request),
            action: AuditActionType.CREATE,
            entityType: 'Component',
            entityId: created.id,
            newValues: created as Record<string, unknown>,
          },
          request,
        );

        return reply.code(201).send({ component: created });
      } catch (err) {
        app.log.error(err);
        return reply.code(500).send({ error: 'failed to create component' });
      }
    },
  );

  app.put(
    '/:id',
    { preHandler: requireAdminOrTA, schema: updateComponentSchema },
    async (request, reply) => {
      const params = request.params as { id?: string };
      const id = params?.id;
      const body = request.body as {
        name?: string;
        description?: string;
        imageUrl?: string;
        availableQuantity?: number;
        totalQuantity?: number;
        category?: string;
        location?: string;
      };

      if (!id) {
        return reply.code(400).send({ error: 'component id is required' });
      }

      const name = body?.name?.trim();
      const description = body?.description?.trim();
      const imageUrl = body?.imageUrl?.trim();
      const availableQuantity = body?.availableQuantity;
      const totalQuantity = body?.totalQuantity;
      const category = body?.category?.trim();
      const location = body?.location?.trim();

      if (totalQuantity !== undefined && (typeof totalQuantity !== 'number' || totalQuantity < 0)) {
        return reply.code(400).send({ error: 'totalQuantity must be a non-negative number' });
      }

      if (
        availableQuantity !== undefined &&
        (typeof availableQuantity !== 'number' || availableQuantity < 0)
      ) {
        return reply.code(400).send({ error: 'availableQuantity must be a non-negative number' });
      }

      if (category !== undefined && category !== null && category !== '') {
        if (!categoryValues.includes(category as CategoryValue)) {
          return reply.code(400).send({
            error: `invalid category. Must be one of ${categoryValues.join(
              ', ',
            )} (use Others if none apply)`,
          });
        }
      }

      if (location !== undefined && location !== null && location !== '') {
        if (!locationValues.includes(location as LocationValue)) {
          return reply
            .code(400)
            .send({ error: 'invalid location. Must be one of IoT Lab, Robo Lab, VLSI Lab' });
        }
      }

      if (imageUrl !== undefined && imageUrl !== null && imageUrl !== '') {
        if (!isValidHttpUrl(imageUrl)) {
          return reply.code(400).send({ error: 'imageUrl must be a valid HTTP or HTTPS URL' });
        }
      }

      try {
        const existingComponent = await db.query.component.findFirst({
          where: (c, { eq }) => eq(c.id, id),
        });

        if (!existingComponent) {
          return reply.code(404).send({ error: 'component not found' });
        }

        const nextTotalQuantity =
          totalQuantity !== undefined ? totalQuantity : existingComponent.totalQuantity;

        const nextAvailableQuantity =
          availableQuantity !== undefined
            ? availableQuantity
            : totalQuantity !== undefined
              ? totalQuantity
              : existingComponent.availableQuantity;

        if (nextAvailableQuantity > nextTotalQuantity) {
          return reply
            .code(400)
            .send({ error: 'availableQuantity cannot be greater than totalQuantity' });
        }

        const updateData: Record<string, unknown> = {};
        if (name !== undefined) updateData.name = name;
        if (description !== undefined) updateData.description = description || null;
        if (imageUrl !== undefined) updateData.imageUrl = imageUrl || null;
        if (totalQuantity !== undefined) updateData.totalQuantity = nextTotalQuantity;
        if (availableQuantity !== undefined || totalQuantity !== undefined) {
          updateData.availableQuantity = nextAvailableQuantity;
        }
        if (category !== undefined)
          updateData.category = category ? (category as CategoryValue) : null;
        if (location !== undefined)
          updateData.location = location ? toLocationEnum(location) : null;
        updateData.updatedAt = new Date().toISOString();

        const [updated] = await db
          .update(component)
          .set(updateData as typeof component.$inferInsert)
          .where(eq(component.id, id))
          .returning();

        if (!updated) {
          return reply.code(500).send({ error: 'failed to update component' });
        }

        await logAudit(
          {
            userId: getUserIdFromRequest(request),
            action: AuditActionType.UPDATE,
            entityType: 'Component',
            entityId: updated.id,
            oldValues: existingComponent as Record<string, unknown>,
            newValues: updated as Record<string, unknown>,
          },
          request,
        );

        return reply.send({ component: updated });
      } catch (err) {
        app.log.error(err);
        return reply.code(500).send({ error: 'failed to update component' });
      }
    },
  );

  app.delete('/:id', { preHandler: requireAdminOrTA }, async (request, reply) => {
    const params = request.params as { id?: string };
    const id = params?.id;

    if (!id) {
      return reply.code(400).send({ error: 'component id is required' });
    }

    try {
      const existing = await db.query.component.findFirst({
        where: (c, { eq }) => eq(c.id, id),
      });

      if (!existing) {
        return reply.code(404).send({ error: 'component not found' });
      }

      const usageRows = await db
        .select({ count: sql<number>`count(*)::int` })
        .from(requestItem)
        .where(eq(requestItem.componentId, id));

      if ((usageRows[0]?.count ?? 0) > 0) {
        return reply.code(400).send({
          error: 'component cannot be deleted because it is used in one or more requests',
        });
      }

      await db.delete(component).where(eq(component.id, id));

      await logAudit(
        {
          userId: getUserIdFromRequest(request),
          action: AuditActionType.DELETE,
          entityType: 'Component',
          entityId: id,
          oldValues: existing as Record<string, unknown>,
        },
        request,
      );

      return reply.code(204).send();
    } catch (err) {
      app.log.error(err);
      return reply.code(500).send({ error: 'failed to delete component' });
    }
  });
};

export default componentsRoutes;
