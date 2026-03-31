import 'dotenv/config';
import Fastify from 'fastify';
import type { FastifyRequest } from 'fastify';
import cors from '@fastify/cors';
import helmet from '@fastify/helmet';
import rateLimit from '@fastify/rate-limit';
import jwt from '@fastify/jwt';
import { pool } from './drizzle/db.js';
import routes from './routes/index.js';

export async function buildApp() {
  const isTest =
    process.env.NODE_ENV === 'test' ||
    process.argv.some((arg) => arg.includes('--test') || arg.includes('test'));
  const isProd = process.env.NODE_ENV === 'production';
  const app = Fastify({
    bodyLimit: 512 * 1024,
    logger: isProd
      ? true
      : isTest
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

  await app.register(helmet);

  if (!isTest) {
    await app.register(rateLimit, {
      global: true,
      max: 100,
      timeWindow: '1 minute',
      keyGenerator: (request) => {
        return request.headers.authorization || `ip:${request.ip}`;
      },
      errorResponseBuilder: (_request, context) => ({
        error: `Too many requests. Please wait ${Math.ceil(context.ttl / 1000)} seconds before retrying.`,
      }),
    });
  }

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
    sign: {
      expiresIn: '7d',
    },
  });

  app.setSchemaErrorFormatter((errors) => {
    const first = errors[0];
    if (first?.params && 'missingProperty' in first.params) {
      const field = first.params.missingProperty as string;
      return new Error(`${field} is required`);
    }
    if (first?.instancePath) {
      const field = first.instancePath.replace(/^\//, '');
      return new Error(`${field} ${first.message ?? 'is invalid'}`);
    }
    return new Error(first?.message ?? 'Validation error');
  });

  if (isProd) {
    app.addHook('onRequest', async (request, reply) => {
      if (request.headers['x-forwarded-proto'] === 'http') {
        const host = request.headers.host;
        return reply.code(301).redirect(`https://${host ?? ''}${request.url}`);
      }
    });
  }

  app.setErrorHandler((error, request, reply) => {
    if (reply.raw.headersSent || reply.sent) {
      request.log.error({ err: error }, 'Unhandled error after headers sent');
      return;
    }

    const err = error as { statusCode?: number; code?: string; message?: string };
    const statusCode =
      err.statusCode && err.statusCode >= 400 && err.statusCode < 600 ? err.statusCode : 500;

    if (statusCode >= 500) {
      request.log.error(error);
    } else {
      request.log.warn({ code: err.code, statusCode }, err.message);
    }

    void reply.code(statusCode).send({
      error: err.message ?? (statusCode === 500 ? 'Internal Server Error' : 'Request failed'),
    });
  });

  app.get('/', () => {
    return { message: 'IIITNR Inventory App Backend' };
  });

  app.get('/health', async (_, reply) => {
    try {
      await pool.query('SELECT 1');

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

    await pool.end();
  });

  return app;
}
