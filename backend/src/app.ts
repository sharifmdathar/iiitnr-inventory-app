import 'dotenv/config';
import fs from 'node:fs';
import { join } from 'node:path';
import Fastify from 'fastify';
import type { FastifyInstance, FastifyRequest, FastifyReply } from 'fastify';
import cors from '@fastify/cors';
import helmet from '@fastify/helmet';
import rateLimit from '@fastify/rate-limit';
import jwt from '@fastify/jwt';
import multipart from '@fastify/multipart';
import staticFiles from '@fastify/static';
import { FastifySSEPlugin } from 'fastify-sse-v2';
import { pool } from './drizzle/db.js';
import routes from './routes/index.js';
import imagesRoutes from './routes/images.js';
import { startRequestExpirySweep } from './services/request-expiry.js';
import { notifyRequestsUpdated } from './utils/events.js';
import { DomainError } from './utils/errors.js';

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
    .map((origin) => {
      try {
        return new URL(origin).origin;
      } catch {
        return origin.replace(/\/$/, '');
      }
    })
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
    await reply.redirect(`https://${host ?? ''}${request.url}`);
  }
}

function handleError(error: Error, request: FastifyRequest, reply: FastifyReply) {
  if (reply.raw.headersSent || reply.sent) {
    request.log.error({ err: error }, 'Unhandled error after headers sent');
    return;
  }

  let statusCode = 500;
  let message = 'Internal Server Error';
  let code: string | undefined;

  const isDomainError = error instanceof DomainError || (error as any).isDomainError === true;

  if (isDomainError) {
    const err = error as DomainError;
    statusCode = err.statusCode;
    message = err.message;
    code = err.code;
  } else {
    const err = error as ErrorWithStatus;
    if (isValidHttpErrorCode(err.statusCode)) {
      statusCode = err.statusCode;
      message = err.message ?? 'Request failed';
      code = err.code;
    }
  }

  if (statusCode >= 500) {
    request.log.error(error);
  } else {
    request.log.warn({ code, statusCode }, message);
  }

  reply.code(statusCode).send({ error: message, code });
}

function isValidHttpErrorCode(code: number | undefined): code is number {
  return code !== undefined && code >= 400 && code < 600;
}

function handleRootRoute() {
  return { message: 'IIITNR Inventory App Backend' };
}

const APP_VERSION: string = (() => {
  try {
    const pkg = JSON.parse(fs.readFileSync('./package.json', 'utf-8'));
    return typeof pkg.version === 'string' ? pkg.version : 'unknown';
  } catch {
    return 'unknown';
  }
})();

let appReady = false;

export function markAppReady() {
  appReady = true;
}

function handleHealthCheck(reply: FastifyReply) {
  return reply.code(200).send({ status: 'ok' });
}

async function handleReadyCheck(app: FastifyInstance, reply: FastifyReply) {
  if (!appReady) {
    return reply.code(503).send({ status: 'starting' });
  }

  try {
    await pool.query('SELECT 1');
    return reply.code(200).send({ status: 'ready', db: 'up' });
  } catch (error) {
    app.log.error({ err: error }, 'Readiness check DB query failed');
    return reply.code(503).send({ status: 'not_ready', db: 'down' });
  }
}

async function registerPlugins(app: FastifyInstance, env: AppEnvironment) {
  const allowedOrigins = getAllowedOrigins();

  await app.register(cors, {
    origin: buildCorsOrigin(env, allowedOrigins),
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization'],
    exposedHeaders: ['Last-Modified'],
  });

  await app.register(helmet, {
    crossOriginResourcePolicy: { policy: 'cross-origin' },
  });

  if (!env.isTest) {
    await app.register(rateLimit, buildRateLimitConfig());
  }

  await app.register(jwt, {
    secret: getJwtSecret(),
    sign: { expiresIn: '7d' },
  });

  await app.register(multipart, {
    limits: { fileSize: 10 * 1024 * 1024 },
  });

  await app.register(FastifySSEPlugin);

  await app.register(staticFiles, {
    root: join(process.cwd(), 'uploads'),
    prefix: '/uploads/',
  });

  await app.register(staticFiles, {
    root: join(process.cwd(), 'uploads'),
    prefix: '/api/uploads/',
    decorateReply: false,
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

  let stopRequestExpirySweep: (() => void) | undefined;
  if (!env.isTest) {
    app.addHook('onReady', (done) => {
      stopRequestExpirySweep = startRequestExpirySweep((err) => {
        app.log.error({ err }, 'Request expiry sweep failed');
        notifyRequestsUpdated();
      });
      done();
    });
  }

  app.addHook('onClose', async () => {
    stopRequestExpirySweep?.();
    if (!env.isTest) {
      await pool.end();
    }
  });
}

function setupRoutes(app: FastifyInstance) {
  app.get('/', handleRootRoute);
  app.get('/health', (_, reply) => handleHealthCheck(reply));
  app.get('/ready', (_, reply) => handleReadyCheck(app, reply));
  app.get('/version', (_, reply) => {
    reply.send({ version: APP_VERSION });
  });
}

export async function buildApp() {
  const env = getAppEnvironment();

  const app = Fastify({
    bodyLimit: 11 * 1024 * 1024,
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
