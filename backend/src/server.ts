import 'dotenv/config';
import Fastify from 'fastify';
import cors from '@fastify/cors';
import jwt from '@fastify/jwt';
import { compare, hash } from 'bcryptjs';
import { UserRole } from '@prisma/client';
import { prisma, pool } from './lib/prisma.js';

const app = Fastify({ logger: true });

await app.register(cors, { origin: true });
const jwtSecret = process.env.JWT_SECRET;
if (!jwtSecret || jwtSecret == "change-me") {
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

    const token = app.jwt.sign({ sub: user.id, role: user.role });

    return reply.code(201).send({ user, token });
  } catch (error) {
    app.log.error(error);
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

  const token = app.jwt.sign(
    { sub: user.id, role: user.role },
    { expiresIn: '1d' },
  );

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

const port = Number(process.env.PORT ?? 4000);

app.addHook('onClose', async () => {
  await prisma.$disconnect();
  await pool.end();
});

try {
  await app.listen({ port, host: '0.0.0.0' });
} catch (err) {
  app.log.error(err);
  process.exit(1);
}
