import type { FastifyPluginAsync } from 'fastify';
import { prisma } from '../lib/prisma.js';
import { requireAuth, isAdminOrTA } from '../middleware/auth.js';
import { RequestStatus, requestStatusValues, UserRole } from '../utils/enums.js';
import type { RequestStatusValue } from '../utils/enums.js';
import type { UserRoleValue } from '../utils/enums.js';

const requestsRoutes: FastifyPluginAsync = async (app) => {
  app.post('/requests', { preHandler: requireAuth }, async (request, reply) => {
    const body = request.body as {
      items?: Array<{ componentId?: string; quantity?: number }>;
      targetFacultyId?: string;
      projectTitle?: string;
    };

    const userId = (request.user as { sub?: string })?.sub;
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
      const faculty = await prisma.user.findUnique({
        where: { id: targetFacultyId, role: UserRole.FACULTY },
        select: { id: true },
      });
      if (!faculty) {
        return reply.code(400).send({ error: 'invalid targetFacultyId' });
      }

      const existingComponents = await prisma.component.findMany({
        where: { id: { in: componentIds } },
        select: { id: true },
      });

      if (existingComponents.length !== componentIds.length) {
        return reply.code(400).send({ error: 'one or more components not found' });
      }

      const createdRequest = await prisma.request.create({
        data: {
          userId,
          targetFacultyId,
          projectTitle,
          items: {
            create: normalizedItems.map((item) => ({
              component: {
                connect: { id: item.componentId as string },
              },
              quantity: item.quantity ?? 0,
            })),
          },
        },
        include: {
          items: {
            include: {
              component: true,
            },
          },
          targetFaculty: {
            select: {
              id: true,
              email: true,
              name: true,
              role: true,
            },
          },
        },
      });

      return reply.code(201).send({ request: createdRequest });
    } catch (err) {
      app.log.error(err);
      return reply.code(500).send({ error: 'failed to create request' });
    }
  });

  app.get('/faculty', { preHandler: requireAuth }, async (_, reply) => {
    try {
      const faculty = await prisma.user.findMany({
        where: { role: UserRole.FACULTY },
        orderBy: { createdAt: 'desc' },
        select: { id: true, email: true, name: true, role: true },
      });
      return reply.send({ faculty });
    } catch (err) {
      app.log.error(err);
      return reply.code(500).send({ error: 'failed to fetch faculty' });
    }
  });

  app.get('/requests', { preHandler: requireAuth }, async (request, reply) => {
    const user = request.user as { sub?: string; role?: UserRoleValue };
    const currentUserId = user?.sub;
    if (!currentUserId) {
      return reply.code(401).send({ error: 'invalid token' });
    }

    const query = request.query as { userId?: string; status?: string };
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

    if (user?.role === UserRole.FACULTY) {
      where.targetFacultyId = currentUserId;
    } else if (isAdminOrTA(user?.role)) {
      if (requestedUserId) {
        where.userId = requestedUserId;
      }
    } else {
      where.userId = currentUserId;
    }

    try {
      const requests = await prisma.request.findMany({
        where,
        orderBy: { createdAt: 'desc' },
        include: {
          items: {
            include: {
              component: true,
            },
          },
          user: {
            select: {
              id: true,
              email: true,
              name: true,
              role: true,
            },
          },
          targetFaculty: {
            select: {
              id: true,
              email: true,
              name: true,
              role: true,
            },
          },
        },
      });

      return reply.send({ requests });
    } catch (err) {
      app.log.error(err);
      return reply.code(500).send({ error: 'failed to fetch requests' });
    }
  });

  app.put('/requests/:id', { preHandler: requireAuth }, async (request, reply) => {
    const params = request.params as { id?: string };
    const id = params?.id;
    const body = request.body as { status?: string };

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

    const user = request.user as { sub?: string; role?: UserRoleValue };
    const currentUserId = user?.sub;
    if (!currentUserId) {
      return reply.code(401).send({ error: 'invalid token' });
    }

    try {
      const existingRequest = await prisma.request.findUnique({
        where: { id },
        include: {
          items: {
            include: {
              component: true,
            },
          },
        },
      });

      if (!existingRequest) {
        return reply.code(404).send({ error: 'request not found' });
      }

      if (existingRequest.status === RequestStatus.PENDING) {
        if (newStatus === RequestStatus.FULFILLED) {
          return reply
            .code(400)
            .send({ error: 'request must be APPROVED before it can be FULFILLED' });
        }

        if (user?.role === UserRole.FACULTY) {
          if (existingRequest.targetFacultyId !== currentUserId) {
            return reply
              .code(403)
              .send({ error: 'forbidden: can only approve/reject requests targeting you' });
          }
        } else if (!isAdminOrTA(user?.role)) {
          return reply
            .code(403)
            .send({ error: 'forbidden: only faculty, admin, or TA can approve/reject requests' });
        }

        const updatedRequest = await prisma.request.update({
          where: { id },
          data: { status: newStatus },
          include: {
            items: {
              include: {
                component: true,
              },
            },
            user: {
              select: {
                id: true,
                email: true,
                name: true,
                role: true,
              },
            },
            targetFaculty: {
              select: {
                id: true,
                email: true,
                name: true,
                role: true,
              },
            },
          },
        });

        return reply.send({ request: updatedRequest });
      } else if (existingRequest.status === RequestStatus.APPROVED) {
        if (newStatus !== RequestStatus.FULFILLED) {
          return reply.code(400).send({ error: 'approved request can only be set to FULFILLED' });
        }

        if (!isAdminOrTA(user?.role)) {
          return reply
            .code(403)
            .send({ error: 'forbidden: only admin or TA can fulfill requests' });
        }

        try {
          const updatedRequest = await prisma.$transaction(async (tx: typeof prisma) => {
            for (const item of existingRequest.items) {
              const result = await tx.component.updateMany({
                where: {
                  id: item.componentId,
                  availableQuantity: {
                    gte: item.quantity,
                  },
                },
                data: {
                  availableQuantity: {
                    decrement: item.quantity,
                  },
                },
              });

              if (result.count === 0) {
                throw new Error(`INSUFFICIENT_QUANTITY:${item.component.name}:${item.quantity}`);
              }
            }

            return tx.request.update({
              where: { id },
              data: { status: newStatus },
              include: {
                items: {
                  include: {
                    component: true,
                  },
                },
                user: {
                  select: {
                    id: true,
                    email: true,
                    name: true,
                    role: true,
                  },
                },
                targetFaculty: {
                  select: {
                    id: true,
                    email: true,
                    name: true,
                    role: true,
                  },
                },
              },
            });
          });

          return reply.send({ request: updatedRequest });
        } catch (error) {
          if (error instanceof Error && error.message.startsWith('INSUFFICIENT_QUANTITY:')) {
            const [, componentName] = error.message.split(':');
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
  });

  app.delete('/requests/:id', { preHandler: requireAuth }, async (request, reply) => {
    const params = request.params as { id?: string };
    const id = params?.id;

    if (!id) {
      return reply.code(400).send({ error: 'request id is required' });
    }

    const user = request.user as { sub?: string; role?: UserRoleValue };
    const currentUserId = user?.sub;
    if (!currentUserId) {
      return reply.code(401).send({ error: 'invalid token' });
    }

    try {
      const existingRequest = await prisma.request.findUnique({
        where: { id },
      });

      if (!existingRequest) {
        return reply.code(404).send({ error: 'request not found' });
      }

      const isOwner = existingRequest.userId === currentUserId;
      const isPrivileged = isAdminOrTA(user?.role);

      if (!isOwner && !isPrivileged) {
        return reply.code(403).send({ error: 'forbidden: cannot delete this request' });
      }

      if (existingRequest.status !== RequestStatus.PENDING) {
        return reply
          .code(400)
          .send({ error: 'request can only be deleted when status is PENDING' });
      }

      await prisma.requestItem.deleteMany({ where: { requestId: id } });
      await prisma.request.delete({ where: { id } });

      return reply.code(204).send();
    } catch (err) {
      app.log.error(err);
      return reply.code(500).send({ error: 'failed to delete request' });
    }
  });
};

export default requestsRoutes;
