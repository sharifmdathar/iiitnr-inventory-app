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

  app.setErrorHandler((error, request, reply) => {
    if (reply.raw.headersSent || reply.sent) {
      request.log.error({ err: error }, 'Unhandled error after headers sent');
      return;
    }

    request.log.error(error);

    const err = error as { statusCode: number; message?: string };
    const statusCode = err.statusCode >= 400 && err.statusCode < 600 ? err.statusCode : 500;

    void reply.code(statusCode).send({
      error: err.message ?? (statusCode === 500 ? 'Internal Server Error' : 'Request failed'),
    });
  });

  app.get('/', async () => {
    return { message: 'IIITNR Inventory App Backend' };
  });

  app.get('/health', async (_, reply) => {
    try {
      await prisma.$queryRaw`SELECT 1`;

      return reply.code(200).send({ status: 'ok', db: 'up' });
    } catch (error) {
      app.log.error({ err: error }, 'Health check DB query failed');
      return reply.code(503).send({ status: 'error', db: 'down' });
    }
  });

  await app.register(routes);

  app.addHook('onClose', async () => {
    if (isTest) {
      return;
    }

    await prisma.$disconnect();
    await pool.end();
  });

  return app;
}
