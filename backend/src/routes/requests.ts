import type { FastifyPluginAsync } from 'fastify';
import { and, desc, eq, inArray } from 'drizzle-orm';
import { db } from '../drizzle/db.js';
import { component, request, requestItem, user } from '../drizzle/schema.js';
import { requireAuth, isAdminOrTA } from '../middleware/auth.js';
import { RequestStatus, requestStatusValues, UserRole } from '../utils/enums.js';
import type { RequestStatusValue, UserRoleValue } from '../utils/enums.js';

const createRequestSchema = {
  body: {
    type: 'object',
    properties: {
      items: {
        type: 'array',
        minItems: 1,
        maxItems: 100,
        items: {
          type: 'object',
          properties: {
            componentId: { type: 'string', maxLength: 100 },
            quantity: { type: 'integer', minimum: 1 },
          },
          additionalProperties: false,
        },
      },
      targetFacultyId: { type: 'string', maxLength: 100 },
      projectTitle: { type: 'string', minLength: 1, maxLength: 300 },
    },
    additionalProperties: false,
  },
} as const;

const updateRequestStatusSchema = {
  body: {
    type: 'object',
    required: ['status'],
    properties: {
      status: { type: 'string', maxLength: 50 },
    },
    additionalProperties: false,
  },
} as const;

const requestsRoutes: FastifyPluginAsync = async (app) => {
  app.post(
    '/requests',
    { preHandler: requireAuth, schema: createRequestSchema },
    async (req, reply) => {
      const body = req.body as {
        items?: Array<{ componentId?: string; quantity?: number }>;
        targetFacultyId?: string;
        projectTitle?: string;
      };

      const userId = (req.user as { sub?: string })?.sub;
      if (!userId) {
        return reply.code(401).send({ error: 'invalid token' });
      }

      const items = body?.items ?? [];
      const targetFacultyId = body?.targetFacultyId?.trim();
      const projectTitle = body?.projectTitle?.trim();

      if (!Array.isArray(items) || items.length === 0) {
        return reply.code(400).send({ error: 'items are required' });
      }
      if (!targetFacultyId) {
        return reply.code(400).send({ error: 'targetFacultyId is required' });
      }
      if (!projectTitle) {
        return reply.code(400).send({ error: 'projectTitle is required' });
      }

      const normalizedItems = items.map((item) => ({
        componentId: item?.componentId?.trim(),
        quantity: item?.quantity,
      }));

      for (const item of normalizedItems) {
        if (!item.componentId) {
          return reply.code(400).send({ error: 'componentId is required' });
        }
        if (typeof item.quantity !== 'number' || item.quantity <= 0) {
          return reply.code(400).send({ error: 'quantity must be a positive number' });
        }
      }

      const componentIds = normalizedItems.map((item) => item.componentId as string);
      const uniqueComponentIds = new Set(componentIds);
      if (uniqueComponentIds.size !== componentIds.length) {
        return reply.code(400).send({ error: 'duplicate componentId in request' });
      }

      try {
        const facultyRow = await db.query.user.findFirst({
          columns: { id: true },
          where: (u, { eq, and }) => and(eq(u.id, targetFacultyId), eq(u.role, UserRole.FACULTY)),
        });
        if (!facultyRow) {
          return reply.code(400).send({ error: 'invalid targetFacultyId' });
        }

        const existingComponents = await db
          .select({ id: component.id })
          .from(component)
          .where(inArray(component.id, componentIds));

        if (existingComponents.length !== componentIds.length) {
          return reply.code(400).send({ error: 'one or more components not found' });
        }

        const now = new Date().toISOString();
        const requestId = crypto.randomUUID();

        await db.insert(request).values({
          id: requestId,
          userId,
          targetFacultyId,
          projectTitle,
          status: RequestStatus.PENDING,
          createdAt: now,
          updatedAt: now,
        });

        await db.insert(requestItem).values(
          normalizedItems.map((item) => ({
            id: crypto.randomUUID(),
            requestId,
            componentId: item.componentId!,
            quantity: item.quantity ?? 0,
            createdAt: now,
            updatedAt: now,
          })),
        );

        const [createdRequest] = await db.query.request.findMany({
          where: eq(request.id, requestId),
          with: {
            requestItems: { with: { component: true } },
            user_userId: {
              columns: { id: true, email: true, name: true, role: true },
            },
            user_targetFacultyId: {
              columns: { id: true, email: true, name: true, role: true },
            },
          },
        });

        if (!createdRequest) {
          return reply.code(500).send({ error: 'failed to create request' });
        }

        const shaped = {
          ...createdRequest,
          items: createdRequest.requestItems,
          user: createdRequest.user_userId,
          targetFaculty: createdRequest.user_targetFacultyId,
        };
        return reply.code(201).send({ request: shaped });
      } catch (err) {
        app.log.error(err);
        return reply.code(500).send({ error: 'failed to create request' });
      }
    },
  );

  app.get('/faculty', { preHandler: requireAuth }, async (_, reply) => {
    try {
      const faculty = await db
        .select({
          id: user.id,
          email: user.email,
          name: user.name,
          role: user.role,
        })
        .from(user)
        .where(eq(user.role, UserRole.FACULTY))
        .orderBy(desc(user.createdAt));
      return reply.send({ faculty });
    } catch (err) {
      app.log.error(err);
      return reply.code(500).send({ error: 'failed to fetch faculty' });
    }
  });

  app.get('/requests', { preHandler: requireAuth }, async (req, reply) => {
    const currentUser = req.user as { sub?: string; role?: UserRoleValue };
    const currentUserId = currentUser?.sub;
    if (!currentUserId) {
      return reply.code(401).send({ error: 'invalid token' });
    }

    const query = req.query as { userId?: string; status?: string };
    const requestedUserId = query?.userId?.trim();
    const status = query?.status?.trim();

    const where: {
      userId?: string;
      targetFacultyId?: string;
      status?: RequestStatusValue;
    } = {};

    if (status) {
      if (!requestStatusValues.includes(status as RequestStatusValue)) {
        return reply.code(400).send({ error: 'invalid status' });
      }
      where.status = status as RequestStatusValue;
    }

    if (currentUser?.role === UserRole.FACULTY) {
      where.targetFacultyId = currentUserId;
    } else if (isAdminOrTA(currentUser?.role)) {
      if (requestedUserId) {
        where.userId = requestedUserId;
      }
    } else {
      where.userId = currentUserId;
    }

    try {
      const conditions = [];
      if (where.userId) conditions.push(eq(request.userId, where.userId));
      if (where.targetFacultyId)
        conditions.push(eq(request.targetFacultyId, where.targetFacultyId));
      if (where.status) conditions.push(eq(request.status, where.status));
      const whereClause = conditions.length > 0 ? and(...conditions) : undefined;

      const rows = await db.query.request.findMany({
        where: whereClause,
        orderBy: desc(request.createdAt),
        with: {
          requestItems: { with: { component: true } },
          user_userId: {
            columns: { id: true, email: true, name: true, role: true },
          },
          user_targetFacultyId: {
            columns: { id: true, email: true, name: true, role: true },
          },
        },
      });

      const requests = rows.map((r) => ({
        ...r,
        items: r.requestItems,
        user: r.user_userId,
        targetFaculty: r.user_targetFacultyId,
      }));

      return reply.send({ requests });
    } catch (err) {
      app.log.error(err);
      return reply.code(500).send({ error: 'failed to fetch requests' });
    }
  });

  app.put(
    '/requests/:id',
    { preHandler: requireAuth, schema: updateRequestStatusSchema },
    async (req, reply) => {
      const params = req.params as { id?: string };
      const id = params?.id;
      const body = req.body as { status?: string };

      if (!id) {
        return reply.code(400).send({ error: 'request id is required' });
      }

      const status = body?.status?.trim();
      if (!status) {
        return reply.code(400).send({ error: 'status is required' });
      }

      if (!requestStatusValues.includes(status as RequestStatusValue)) {
        return reply.code(400).send({ error: 'invalid status' });
      }

      const newStatus = status as RequestStatusValue;

      if (
        newStatus !== RequestStatus.APPROVED &&
        newStatus !== RequestStatus.REJECTED &&
        newStatus !== RequestStatus.FULFILLED
      ) {
        return reply
          .code(400)
          .send({ error: 'status can only be set to APPROVED, REJECTED, or FULFILLED' });
      }

      const currentUser = req.user as { sub?: string; role?: UserRoleValue };
      const currentUserId = currentUser?.sub;
      if (!currentUserId) {
        return reply.code(401).send({ error: 'invalid token' });
      }

      try {
        const [existingRow] = await db.query.request.findMany({
          where: eq(request.id, id),
          with: { requestItems: { with: { component: true } } },
        });
        const existingRequest = existingRow;

        if (!existingRequest) {
          return reply.code(404).send({ error: 'request not found' });
        }

        if (existingRequest.status === RequestStatus.PENDING) {
          if (newStatus === RequestStatus.FULFILLED) {
            return reply
              .code(400)
              .send({ error: 'request must be APPROVED before it can be FULFILLED' });
          }

          if (currentUser?.role === UserRole.FACULTY) {
            if (existingRequest.targetFacultyId !== currentUserId) {
              return reply
                .code(403)
                .send({ error: 'forbidden: can only approve/reject requests targeting you' });
            }
          } else if (!isAdminOrTA(currentUser?.role)) {
            return reply
              .code(403)
              .send({ error: 'forbidden: only faculty, admin, or TA can approve/reject requests' });
          }

          await db
            .update(request)
            .set({ status: newStatus, updatedAt: new Date().toISOString() })
            .where(eq(request.id, id));

          const [updatedRow] = await db.query.request.findMany({
            where: eq(request.id, id),
            with: {
              requestItems: { with: { component: true } },
              user_userId: {
                columns: { id: true, email: true, name: true, role: true },
              },
              user_targetFacultyId: {
                columns: { id: true, email: true, name: true, role: true },
              },
            },
          });

          const updatedRequest = updatedRow
            ? {
                ...updatedRow,
                items: updatedRow.requestItems,
                user: updatedRow.user_userId,
                targetFaculty: updatedRow.user_targetFacultyId,
              }
            : null;

          return reply.send({ request: updatedRequest });
        } else if (existingRequest.status === RequestStatus.APPROVED) {
          if (newStatus !== RequestStatus.FULFILLED) {
            return reply.code(400).send({ error: 'approved request can only be set to FULFILLED' });
          }

          if (!isAdminOrTA(currentUser?.role)) {
            return reply
              .code(403)
              .send({ error: 'forbidden: only admin or TA can fulfill requests' });
          }

          try {
            await db.transaction(async (tx) => {
              for (const item of existingRequest.requestItems) {
                const comp = await tx.query.component.findFirst({
                  where: (c, { eq, and, gte }) =>
                    and(eq(c.id, item.componentId), gte(c.availableQuantity, item.quantity)),
                });

                if (!comp || comp.availableQuantity < item.quantity) {
                  throw new Error(
                    `INSUFFICIENT_QUANTITY:${item.component?.name ?? 'unknown'}:${item.quantity}`,
                  );
                }

                await tx
                  .update(component)
                  .set({
                    availableQuantity: comp.availableQuantity - item.quantity,
                    updatedAt: new Date().toISOString(),
                  })
                  .where(eq(component.id, item.componentId));
              }

              await tx
                .update(request)
                .set({ status: newStatus, updatedAt: new Date().toISOString() })
                .where(eq(request.id, id));
            });

            const [updatedRow] = await db.query.request.findMany({
              where: eq(request.id, id),
              with: {
                requestItems: { with: { component: true } },
                user_userId: {
                  columns: { id: true, email: true, name: true, role: true },
                },
                user_targetFacultyId: {
                  columns: { id: true, email: true, name: true, role: true },
                },
              },
            });

            const updatedRequest = updatedRow
              ? {
                  ...updatedRow,
                  items: updatedRow.requestItems,
                  user: updatedRow.user_userId,
                  targetFaculty: updatedRow.user_targetFacultyId,
                }
              : null;

            return reply.send({ request: updatedRequest });
          } catch (error) {
            if (error instanceof Error && error.message.startsWith('INSUFFICIENT_QUANTITY:')) {
              const parts = error.message.split(':');
              const componentName = parts[1] ?? 'unknown';
              return reply.code(400).send({
                error: `insufficient quantity for component "${componentName}"`,
              });
            }

            throw error;
          }
        } else {
          return reply.code(400).send({
            error: 'request status can only be updated when status is PENDING or APPROVED',
          });
        }
      } catch (err) {
        app.log.error(err);
        return reply.code(500).send({ error: 'failed to update request' });
      }
    },
  );

  app.delete('/requests/:id', { preHandler: requireAuth }, async (req, reply) => {
    const params = req.params as { id?: string };
    const id = params?.id;

    if (!id) {
      return reply.code(400).send({ error: 'request id is required' });
    }

    const currentUser = req.user as { sub?: string; role?: UserRoleValue };
    const currentUserId = currentUser?.sub;
    if (!currentUserId) {
      return reply.code(401).send({ error: 'invalid token' });
    }

    try {
      const existingRequest = await db.query.request.findFirst({
        where: (r, { eq }) => eq(r.id, id),
      });

      if (!existingRequest) {
        return reply.code(404).send({ error: 'request not found' });
      }

      const isOwner = existingRequest.userId === currentUserId;
      const isPrivileged = isAdminOrTA(currentUser?.role);

      if (!isOwner && !isPrivileged) {
        return reply.code(403).send({ error: 'forbidden: cannot delete this request' });
      }

      if (existingRequest.status !== RequestStatus.PENDING) {
        return reply
          .code(400)
          .send({ error: 'request can only be deleted when status is PENDING' });
      }

      await db.delete(requestItem).where(eq(requestItem.requestId, id));
      await db.delete(request).where(eq(request.id, id));

      return reply.code(204).send();
    } catch (err) {
      app.log.error(err);
      return reply.code(500).send({ error: 'failed to delete request' });
    }
  });
};

export default requestsRoutes;
