import 'dotenv/config';
import Fastify from 'fastify';
import type { FastifyInstance, FastifyRequest, FastifyReply } from 'fastify';
import cors from '@fastify/cors';
import helmet from '@fastify/helmet';
import rateLimit from '@fastify/rate-limit';
import jwt from '@fastify/jwt';
import { pool } from './drizzle/db.js';
import routes from './routes/index.js';

interface AppEnvironment {
  isTest: boolean;
  isProd: boolean;
}

interface ErrorWithStatus {
  statusCode?: number;
  code?: string;
  message?: string;
}

function getAppEnvironment(): AppEnvironment {
  const isTest =
    process.env.NODE_ENV === 'test' ||
    process.argv.some((arg) => arg.includes('--test') || arg.includes('test'));
  const isProd = process.env.NODE_ENV === 'production';

  return { isTest, isProd };
}

function getJwtSecret(): string {
  const jwtSecret = process.env.JWT_SECRET;

  if (!jwtSecret || jwtSecret === 'change-me') {
    throw new Error('JWT_SECRET is required and must not be "change-me"');
  }

  return jwtSecret;
}

function getAllowedOrigins(): string[] | undefined {
  const rawAllowedOrigins = process.env.ALLOWED_ORIGINS;

  if (!rawAllowedOrigins) return undefined;

  return rawAllowedOrigins
    .split(',')
    .map((origin) => origin.trim())
    .filter(Boolean);
}

function buildLoggerConfig(env: AppEnvironment) {
  if (env.isProd) return true;
  if (env.isTest) return false;

  return {
    transport: {
      target: 'pino-pretty',
      options: {
        colorize: true,
        colorizeObjects: true,
        translateTime: 'SYS:standard',
        ignore: 'pid,hostname',
      },
    },
  };
}

function buildCorsOrigin(env: AppEnvironment, allowedOrigins: string[] | undefined) {
  if (!env.isProd && !allowedOrigins) return true;
  return allowedOrigins ?? false;
}

function buildRateLimitConfig() {
  return {
    global: true,
    max: 100,
    timeWindow: '1 minute',
    keyGenerator: (request: FastifyRequest) => {
      return request.headers.authorization || `ip:${request.ip}`;
    },
    errorResponseBuilder: (_request: FastifyRequest, context: { ttl: number }) => ({
      error: `Too many requests. Please wait ${Math.ceil(context.ttl / 1000)} seconds before retrying.`,
    }),
  };
}

async function parseFormUrlEncoded(req: FastifyRequest, body: string) {
  if (req.method === 'DELETE' || !body || body.length === 0) {
    return {};
  }

  const querystring = await import('node:querystring');
  return querystring.parse(body);
}

function formatSchemaError(
  errors: { params?: { missingProperty?: string }; instancePath?: string; message?: string }[],
) {
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
}

function isLoopbackSocket(request: FastifyRequest): boolean {
  const addr = request.socket.remoteAddress;
  return addr === '127.0.0.1' || addr === '::1' || addr === '::ffff:127.0.0.1';
}

async function handleHttpsRedirect(request: FastifyRequest, reply: FastifyReply) {
  if (isLoopbackSocket(request)) {
    return;
  }
  if (request.headers['x-forwarded-proto'] === 'http') {
    const host = request.headers.host;
    return reply.code(301).redirect(`https://${host ?? ''}${request.url}`);
  }
}

function handleError(error: Error, request: FastifyRequest, reply: FastifyReply) {
  if (reply.raw.headersSent || reply.sent) {
    request.log.error({ err: error }, 'Unhandled error after headers sent');
    return;
  }

  const err = error as ErrorWithStatus;
  const statusCode = isValidHttpErrorCode(err.statusCode) ? err.statusCode : 500;

  if (statusCode >= 500) {
    request.log.error(error);
  } else {
    request.log.warn({ code: err.code, statusCode }, err.message);
  }

  const message = err.message ?? (statusCode === 500 ? 'Internal Server Error' : 'Request failed');

  reply.code(statusCode).send({ error: message });
}

function isValidHttpErrorCode(code: number | undefined): code is number {
  return code !== undefined && code >= 400 && code < 600;
}

function handleRootRoute() {
  return { message: 'IIITNR Inventory App Backend' };
}

async function handleHealthCheck(app: FastifyInstance, reply: FastifyReply) {
  try {
    await pool.query('SELECT 1');
    return reply.code(200).send({ status: 'ok', db: 'up' });
  } catch (error) {
    app.log.error({ err: error }, 'Health check DB query failed');
    return reply.code(503).send({ status: 'error', db: 'down' });
  }
}

async function registerPlugins(app: FastifyInstance, env: AppEnvironment) {
  const allowedOrigins = getAllowedOrigins();

  await app.register(cors, {
    origin: buildCorsOrigin(env, allowedOrigins),
  });

  await app.register(helmet);

  if (!env.isTest) {
    await app.register(rateLimit, buildRateLimitConfig());
  }

  await app.register(jwt, {
    secret: getJwtSecret(),
    sign: { expiresIn: '7d' },
  });
}

function setupContentParsers(app: FastifyInstance) {
  app.addContentTypeParser(
    'application/x-www-form-urlencoded',
    { parseAs: 'string' },
    parseFormUrlEncoded,
  );
}

function setupErrorHandling(app: FastifyInstance) {
  app.setSchemaErrorFormatter(formatSchemaError);
  app.setErrorHandler(handleError);
}

function setupHooks(app: FastifyInstance, env: AppEnvironment) {
  if (env.isProd) {
    app.addHook('onRequest', handleHttpsRedirect);
  }

  app.addHook('onClose', async () => {
    if (!env.isTest) {
      await pool.end();
    }
  });
}

function setupRoutes(app: FastifyInstance) {
  app.get('/', handleRootRoute);
  app.get('/health', (_, reply) => handleHealthCheck(app, reply));
}

export async function buildApp() {
  const env = getAppEnvironment();

  const app = Fastify({
    bodyLimit: 512 * 1024,
    logger: buildLoggerConfig(env),
  });

  await registerPlugins(app, env);
  setupContentParsers(app);
  setupErrorHandling(app);
  setupHooks(app, env);
  setupRoutes(app);
  await app.register(routes);

  return app;
}
