import type { FastifyPluginAsync } from 'fastify';
import { prisma } from '../lib/prisma.js';
import { requireAuth, requireAdminOrTA } from '../middleware/auth.js';
import { categoryValues, locationValues, toLocationEnum } from '../utils/enums.js';
import type { CategoryValue } from '../utils/enums.js';
import type { LocationValue } from '../utils/enums.js';

const componentsRoutes: FastifyPluginAsync = async (app) => {
  app.get('/', { preHandler: requireAuth }, async (_, reply) => {
    try {
      const components = await prisma.component.findMany({
        orderBy: { createdAt: 'desc' },
      });
      return reply.send({ components });
    } catch (err) {
      app.log.error(err);
      return reply.code(500).send({ error: 'failed to fetch components' });
    }
  });

  app.get('/:id', { preHandler: requireAuth }, async (request, reply) => {
    const params = request.params as { id?: string };
    const id = params?.id;

    if (!id) {
      return reply.code(400).send({ error: 'component id is required' });
    }

    try {
      const component = await prisma.component.findUnique({
        where: { id },
      });

      if (!component) {
        return reply.code(404).send({ error: 'component not found' });
      }

      return reply.send({ component });
    } catch (err) {
      app.log.error(err);
      return reply.code(500).send({ error: 'failed to fetch component' });
    }
  });

  app.post('/', { preHandler: requireAdminOrTA }, async (request, reply) => {
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

    try {
      const component = await prisma.component.create({
        data: {
          name,
          description: description || null,
          imageUrl: imageUrl || null,
          totalQuantity,
          availableQuantity: availableQuantity ? availableQuantity : totalQuantity,
          category: category ? (category as CategoryValue) : null,
          location: location ? toLocationEnum(location) : null,
        },
      });

      return reply.code(201).send({ component });
    } catch (err) {
      app.log.error(err);
      return reply.code(500).send({ error: 'failed to create component' });
    }
  });

  app.put('/:id', { preHandler: requireAdminOrTA }, async (request, reply) => {
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

    try {
      const existingComponent = await prisma.component.findUnique({
        where: { id },
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

      const component = await prisma.component.update({
        where: { id },
        data: {
          ...(name !== undefined && { name }),
          ...(description !== undefined && { description: description || null }),
          ...(imageUrl !== undefined && { imageUrl: imageUrl || null }),
          ...(totalQuantity !== undefined && { totalQuantity: nextTotalQuantity }),
          ...((availableQuantity !== undefined || totalQuantity !== undefined) && {
            availableQuantity: nextAvailableQuantity,
          }),
          ...(category !== undefined && {
            category: category ? (category as CategoryValue) : null,
          }),
          ...(location !== undefined && {
            location: location ? toLocationEnum(location) : null,
          }),
        },
      });

      return reply.send({ component });
    } catch (err) {
      app.log.error(err);
      return reply.code(500).send({ error: 'failed to update component' });
    }
  });

  app.delete('/:id', { preHandler: requireAdminOrTA }, async (request, reply) => {
    const params = request.params as { id?: string };
    const id = params?.id;

    if (!id) {
      return reply.code(400).send({ error: 'component id is required' });
    }

    try {
      const existingComponent = await prisma.component.findUnique({
        where: { id },
      });

      if (!existingComponent) {
        return reply.code(404).send({ error: 'component not found' });
      }

      const usageCount = await prisma.requestItem.count({
        where: { componentId: id },
      });

      if (usageCount > 0) {
        return reply.code(400).send({
          error: 'component cannot be deleted because it is used in one or more requests',
        });
      }

      await prisma.component.delete({
        where: { id },
      });

      return reply.code(204).send();
    } catch (err) {
      app.log.error(err);
      return reply.code(500).send({ error: 'failed to delete component' });
    }
  });
};

export default componentsRoutes;
