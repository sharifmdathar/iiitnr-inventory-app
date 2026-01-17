import 'dotenv/config';
import Fastify from 'fastify';
import cors from '@fastify/cors';
import jwt from '@fastify/jwt';
import { compare, hash } from 'bcryptjs';
import { UserRole } from '@prisma/client';
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

  const jwtSecret = process.env.JWT_SECRET;
  if (!jwtSecret || jwtSecret === 'change-me') {
    throw new Error('JWT_SECRET is required and must not be "change-me"');
  }

  await app.register(jwt, {
    secret: jwtSecret,
  });

  app.get('/', async () => {
    return { message: 'Hello World from Fastify' };
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
      preHandler: requireAdminOrTA,
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

      try {
        const component = await prisma.component.create({
          data: {
            name,
            description: description || null,
            quantity: quantity ?? 0,
            category: category || null,
            location: location || null,
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
            ...(category !== undefined && { category: category || null }),
            ...(location !== undefined && { location: location || null }),
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

  app.addHook('onClose', async () => {
    await prisma.$disconnect();
    await pool.end();
  });

  return app;
}
