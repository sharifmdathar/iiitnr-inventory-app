import 'dotenv/config';
import Fastify from 'fastify';
import type { FastifyRequest } from 'fastify';
import cors from '@fastify/cors';
import jwt from '@fastify/jwt';
import { prisma, pool } from './lib/prisma.js';
import routes from './routes/index.js';

export async function buildApp() {
  const isTest =
    process.env.NODE_ENV === 'test' ||
    process.argv.some((arg) => arg.includes('--test') || arg.includes('test'));
  const isProd = process.env.NODE_ENV === 'production';
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

  const rawAllowedOrigins = process.env.ALLOWED_ORIGINS;
  const allowedOrigins = rawAllowedOrigins
    ? rawAllowedOrigins
        .split(',')
        .map((origin) => origin.trim())
        .filter(Boolean)
    : undefined;

  await app.register(cors, {
    origin: !isProd && !allowedOrigins ? true : (allowedOrigins ?? false),
  });

  app.addContentTypeParser(
    'application/x-www-form-urlencoded',
    { parseAs: 'string' },
    async (req: FastifyRequest, body: string) => {
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

  await app.register(routes);

  app.addHook('onClose', async () => {
    await prisma.$disconnect();
    await pool.end();
  });

  return app;
}
