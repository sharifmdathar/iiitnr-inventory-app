import 'dotenv/config';
import Fastify from 'fastify';
import cors from '@fastify/cors';
import jwt from '@fastify/jwt';
import { compare, hash } from 'bcryptjs';
import { OAuth2Client } from 'google-auth-library';
import {
  UserRole,
  RequestStatus,
  ComponentCategory,
  Location as PrismaLocation,
} from '@prisma/client';
import { prisma, pool } from './lib/prisma.js';

export async function buildApp() {
  const isTest =
    process.env.NODE_ENV === 'test' ||
    process.argv.some((arg) => arg.includes('--test') || arg.includes('test'));
  const app = Fastify({
    logger: isTest
      ? false
      : {
          transport: {
            target: 'pino-pretty',
            options: {
              colorize: true,
              colorizeObjects: true,
              translateTime: 'SYS:standard',
              ignore: 'pid,hostname',
            },
          },
        },
  });

  await app.register(cors, { origin: true });

  app.addContentTypeParser(
    'application/x-www-form-urlencoded',
    { parseAs: 'string' },
    async (req: any, body: any) => {
      if (req.method === 'DELETE' || !body || body.length === 0) {
        return {};
      }
      const querystring = await import('node:querystring');
      return querystring.parse(body);
    },
  );

  const jwtSecret = process.env.JWT_SECRET;
  if (!jwtSecret || jwtSecret === 'change-me') {
    throw new Error('JWT_SECRET is required and must not be "change-me"');
  }

  await app.register(jwt, {
    secret: jwtSecret,
  });

  app.get('/', async () => {
    return { message: 'IIITNR Inventory App Backend' };
  });

  app.get('/health', async () => {
    return { status: 'ok' };
  });

  app.post('/auth/register', async (request, reply) => {
    const body = request.body as {
      email?: string;
      password?: string;
      name?: string;
      role?: UserRole;
    };

    const email = body?.email?.trim();
    const password = body?.password;
    const name = body?.name?.trim();
    const role = body?.role;

    if (!email || !password) {
      return reply.code(400).send({ error: 'email and password are required' });
    }

    if (password.length < 3) {
      return reply.code(400).send({ error: 'password must be at least 3 characters' });
    }

    if (role === UserRole.ADMIN) {
      return reply.code(400).send({ error: 'invalid role' });
    }

    if (role && !Object.values(UserRole).includes(role)) {
      return reply.code(400).send({ error: 'invalid role' });
    }

    const passwordHash = await hash(password, 12);

    try {
      const user = await prisma.user.create({
        data: {
          email,
          name,
          passwordHash,
          role: role ?? UserRole.STUDENT,
        },
        select: {
          id: true,
          email: true,
          name: true,
          role: true,
          createdAt: true,
        },
      });

      const token = app.jwt.sign({ sub: user.id, role: user.role }, { expiresIn: '1d' });

      return reply.code(201).send({ user, token });
    } catch (err) {
      app.log.error(err);
      return reply.code(400).send({ error: 'email already in use' });
    }
  });

  app.post('/auth/login', async (request, reply) => {
    const body = request.body as { email?: string; password?: string };
    const email = body?.email?.trim();
    const password = body?.password;

    if (!email || !password) {
      return reply.code(400).send({ error: 'email and password are required' });
    }

    const user = await prisma.user.findUnique({
      where: { email },
      select: {
        id: true,
        email: true,
        name: true,
        role: true,
        passwordHash: true,
      },
    });

    if (!user) {
      return reply.code(401).send({ error: 'invalid credentials' });
    }

    if (!user.passwordHash) {
      return reply.code(401).send({ error: 'this account uses Google Sign-In' });
    }

    const matches = await compare(password, user.passwordHash);
    if (!matches) {
      return reply.code(401).send({ error: 'invalid credentials' });
    }

    const token = app.jwt.sign({ sub: user.id, role: user.role }, { expiresIn: '1d' });

    return reply.send({
      user: {
        id: user.id,
        email: user.email,
        name: user.name,
        role: user.role,
      },
      token,
    });
  });

  app.get(
    '/auth/me',
    {
      preHandler: async (request) => {
        await request.jwtVerify();
      },
    },
    async (request, reply) => {
      const userId = (request.user as { sub?: string })?.sub;
      if (!userId) {
        return reply.code(401).send({ error: 'invalid token' });
      }

      const user = await prisma.user.findUnique({
        where: { id: userId },
        select: {
          id: true,
          email: true,
          name: true,
          role: true,
        },
      });

      if (!user) {
        return reply.code(401).send({ error: 'invalid token' });
      }

      return reply.send({ user });
    },
  );

  app.post('/auth/google', async (request, reply) => {
    const body = request.body as { idToken?: string };
    const idToken = body?.idToken;

    if (!idToken) {
      return reply.code(400).send({ error: 'idToken is required' });
    }

    const clientId = process.env.GOOGLE_CLIENT_ID;
    if (!clientId) {
      return reply.code(500).send({ error: 'Google OAuth not configured' });
    }

    const client = new OAuth2Client(clientId);

    try {
      const ticket = await client.verifyIdToken({
        idToken,
        audience: clientId,
      });

      const payload = ticket.getPayload();
      if (!payload) {
        app.log.error('Google token verification returned null payload');
        return reply.code(401).send({ error: 'invalid Google token' });
      }

      const googleId = payload.sub;
      const email = payload.email;
      const name = payload.name || payload.given_name || null;
      const emailVerified = payload.email_verified;

      if (!email) {
        app.log.error('Google Sign-In: No email in token payload');
        return reply.code(400).send({ error: 'No email found in Google account' });
      }

      const allowedDomain = process.env.ALLOWED_EMAIL_DOMAIN || '@iiitnr.edu.in';
      if (!email.toLowerCase().endsWith(allowedDomain.toLowerCase())) {
        app.log.warn(
          `Google Sign-In: Email domain not allowed - email: ${email}, allowedDomain: ${allowedDomain}`,
        );
        return reply.code(403).send({
          error: `Only ${allowedDomain} email addresses are allowed. Your email (${email}) does not match the required domain.`,
        });
      }

      const allowUnverifiedEmail = process.env.ALLOW_UNVERIFIED_EMAIL === 'true';

      if (!emailVerified) {
        if (!allowUnverifiedEmail) {
          return reply.code(400).send({
            error:
              'Google account email is not verified. Please verify your email address in your Google account settings, or set ALLOW_UNVERIFIED_EMAIL=true in .env for development.',
          });
        }
      }

      let user = await prisma.user.findFirst({
        where: {
          OR: [{ googleId: googleId }, { email: email }],
        },
        select: {
          id: true,
          email: true,
          name: true,
          role: true,
          googleId: true,
        },
      });

      if (user) {
        if (!user.googleId || (name && user.name !== name)) {
          user = await prisma.user.update({
            where: { id: user.id },
            data: {
              googleId: user.googleId || googleId,
              name: name || user.name,
            },
            select: {
              id: true,
              email: true,
              name: true,
              role: true,
              googleId: true,
            },
          });
        }
      } else {
        user = await prisma.user.create({
          data: {
            email,
            name,
            googleId,
            role: UserRole.STUDENT,
          },
          select: {
            id: true,
            email: true,
            name: true,
            role: true,
            googleId: true,
          },
        });
      }

      const token = app.jwt.sign({ sub: user.id, role: user.role }, { expiresIn: '1d' });

      return reply.send({
        user: {
          id: user.id,
          email: user.email,
          name: user.name,
          role: user.role,
        },
        token,
      });
    } catch (err: any) {
      app.log.error('Google Sign-In error:', err);

      if (err.message?.includes('audience') || err.code === 'auth/id-token-audience-mismatch') {
        return reply.code(400).send({
          error:
            'Token audience mismatch. The Web application Client ID in the backend must match the serverClientId used in the app.',
        });
      }

      const errorMessage = err.message || err.toString() || 'unknown error';
      return reply.code(400).send({ error: `Invalid Google token: ${errorMessage}` });
    }
  });

  const requireAuth = async (request: any, reply: any) => {
    await request.jwtVerify();
  };

  const requireAdminOrTA = async (request: any, reply: any) => {
    await request.jwtVerify();
    const userRole = (request.user as { role?: UserRole })?.role;
    if (userRole !== UserRole.ADMIN && userRole !== UserRole.TA) {
      return reply.code(403).send({ error: 'forbidden: admin or TA role required' });
    }
  };

  const isAdminOrTA = (role?: UserRole) => role === UserRole.ADMIN || role === UserRole.TA;

  const requestStatusValues = Object.values(RequestStatus);
  type RequestStatusValue = RequestStatus;

  const categoryValues = Object.values(ComponentCategory);
  type CategoryValue = ComponentCategory;

  const locationEnumValues = Object.values(PrismaLocation);
  const locationValues = locationEnumValues.map((v) => v.replace(/_/g, ' '));
  type LocationValue = string;

  const toLocationEnum = (label: string): string => label.replace(/\s+/g, '_');
  const fromLocationEnum = (value: string | null): string | null =>
    value ? value.replace(/_/g, ' ') : value;

  app.get(
    '/components',
    {
      preHandler: requireAuth,
    },
    async (request, reply) => {
      try {
        const components = await prisma.component.findMany({
          orderBy: { createdAt: 'desc' },
        });
        return reply.send({ components });
      } catch (err) {
        app.log.error(err);
        return reply.code(500).send({ error: 'failed to fetch components' });
      }
    },
  );

  app.get(
    '/components/:id',
    {
      preHandler: requireAuth,
    },
    async (request, reply) => {
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
    },
  );

  app.post(
    '/components',
    {
      preHandler: requireAdminOrTA,
    },
    async (request, reply) => {
      const body = request.body as {
        name?: string;
        description?: string;
        quantity?: number;
        category?: string;
        location?: string;
      };

      const name = body?.name?.trim();
      const description = body?.description?.trim();
      const quantity = body?.quantity;
      const category = body?.category?.trim();
      const location = body?.location?.trim();

      if (!name) {
        return reply.code(400).send({ error: 'name is required' });
      }

      if (quantity !== undefined && (typeof quantity !== 'number' || quantity < 0)) {
        return reply.code(400).send({ error: 'quantity must be a non-negative number' });
      }

      if (category && !categoryValues.includes(category as CategoryValue)) {
        return reply.code(400).send({
          error:
            'invalid category. Must be one of ' +
            categoryValues.join(', ') +
            ' (use Others if none apply)',
        });
      }

      if (location && !locationValues.includes(location as LocationValue)) {
        return reply
          .code(400)
          .send({ error: 'invalid location. Must be one of ' + locationValues.join(', ') });
      }

      try {
        const component = await prisma.component.create({
          data: {
            name,
            description: description || null,
            quantity: quantity ?? 0,
            category: category ? (category as CategoryValue) : null,
            location: location ? (toLocationEnum(location) as any) : null,
          },
        });

        return reply.code(201).send({ component });
      } catch (err) {
        app.log.error(err);
        return reply.code(500).send({ error: 'failed to create component' });
      }
    },
  );

  app.put(
    '/components/:id',
    {
      preHandler: requireAdminOrTA,
    },
    async (request, reply) => {
      const params = request.params as { id?: string };
      const id = params?.id;
      const body = request.body as {
        name?: string;
        description?: string;
        quantity?: number;
        category?: string;
        location?: string;
      };

      if (!id) {
        return reply.code(400).send({ error: 'component id is required' });
      }

      const name = body?.name?.trim();
      const description = body?.description?.trim();
      const quantity = body?.quantity;
      const category = body?.category?.trim();
      const location = body?.location?.trim();

      if (quantity !== undefined && (typeof quantity !== 'number' || quantity < 0)) {
        return reply.code(400).send({ error: 'quantity must be a non-negative number' });
      }

      if (category !== undefined && category !== null && category !== '') {
        if (!categoryValues.includes(category as CategoryValue)) {
          return reply.code(400).send({
            error:
              'invalid category. Must be one of ' +
              categoryValues.join(', ') +
              ' (use Others if none apply)',
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

        const component = await prisma.component.update({
          where: { id },
          data: {
            ...(name !== undefined && { name }),
            ...(description !== undefined && { description: description || null }),
            ...(quantity !== undefined && { quantity }),
            ...(category !== undefined && {
              category: category ? (category as CategoryValue) : null,
            }),
            ...(location !== undefined && {
              location: location ? (toLocationEnum(location) as any) : null,
            }),
          },
        });

        return reply.send({ component });
      } catch (err) {
        app.log.error(err);
        return reply.code(500).send({ error: 'failed to update component' });
      }
    },
  );

  app.delete(
    '/components/:id',
    {
      preHandler: requireAdminOrTA,
    },
    async (request, reply) => {
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

        await prisma.component.delete({
          where: { id },
        });

        return reply.code(204).send();
      } catch (err) {
        app.log.error(err);
        return reply.code(500).send({ error: 'failed to delete component' });
      }
    },
  );

  app.post(
    '/requests',
    {
      preHandler: requireAuth,
    },
    async (request, reply) => {
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

        const createdRequest = await (prisma as any).request.create({
          data: {
            userId,
            targetFacultyId,
            projectTitle,
            items: {
              create: normalizedItems.map((item) => ({
                componentId: item.componentId,
                quantity: item.quantity!,
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
    },
  );

  app.get(
    '/faculty',
    {
      preHandler: requireAuth,
    },
    async (_, reply) => {
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
    },
  );

  app.get(
    '/requests',
    {
      preHandler: requireAuth,
    },
    async (request, reply) => {
      const user = request.user as { sub?: string; role?: UserRole };
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
      } else if (isAdminOrTA(user?.role) && requestedUserId) {
        where.userId = requestedUserId;
      } else {
        where.userId = currentUserId;
      }

      try {
        const requests = await (prisma as any).request.findMany({
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

        if (user?.role === UserRole.FACULTY) {
          requests.sort((a: any, b: any) => {
            const aIsPending = a.status === RequestStatus.PENDING;
            const bIsPending = b.status === RequestStatus.PENDING;

            if (aIsPending && !bIsPending) return -1;
            if (!aIsPending && bIsPending) return 1;

            return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
          });
        }

        return reply.send({ requests });
      } catch (err) {
        app.log.error(err);
        return reply.code(500).send({ error: 'failed to fetch requests' });
      }
    },
  );

  app.put(
    '/requests/:id',
    {
      preHandler: requireAuth,
    },
    async (request, reply) => {
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

      if (newStatus !== RequestStatus.APPROVED && newStatus !== RequestStatus.REJECTED) {
        return reply.code(400).send({ error: 'status can only be set to APPROVED or REJECTED' });
      }

      const user = request.user as { sub?: string; role?: UserRole };
      const currentUserId = user?.sub;
      if (!currentUserId) {
        return reply.code(401).send({ error: 'invalid token' });
      }

      try {
        const existingRequest = await (prisma as any).request.findUnique({
          where: { id },
          select: {
            status: true,
            targetFacultyId: true,
          },
        });

        if (!existingRequest) {
          return reply.code(404).send({ error: 'request not found' });
        }

        if (existingRequest.status !== RequestStatus.PENDING) {
          return reply
            .code(400)
            .send({ error: 'request status can only be updated when status is PENDING' });
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

        const updatedRequest = await (prisma as any).request.update({
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
      } catch (err) {
        app.log.error(err);
        return reply.code(500).send({ error: 'failed to update request' });
      }
    },
  );

  app.delete(
    '/requests/:id',
    {
      preHandler: requireAuth,
    },
    async (request, reply) => {
      const params = request.params as { id?: string };
      const id = params?.id;

      if (!id) {
        return reply.code(400).send({ error: 'request id is required' });
      }

      const user = request.user as { sub?: string; role?: UserRole };
      const currentUserId = user?.sub;
      if (!currentUserId) {
        return reply.code(401).send({ error: 'invalid token' });
      }

      try {
        const existingRequest = await (prisma as any).request.findUnique({
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

        await (prisma as any).requestItem.deleteMany({ where: { requestId: id } });
        await (prisma as any).request.delete({ where: { id } });

        return reply.code(204).send();
      } catch (err) {
        app.log.error(err);
        return reply.code(500).send({ error: 'failed to delete request' });
      }
    },
  );

  app.addHook('onClose', async () => {
    await prisma.$disconnect();
    await pool.end();
  });

  return app;
}
