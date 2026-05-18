import type { FastifyPluginCallback, FastifyRequest, FastifyReply } from 'fastify';
import { eq, desc, sql, and, type SQL } from 'drizzle-orm';
import { db } from '../drizzle/db.js';
import { auditLog, user } from '../drizzle/schema.js';
import { requireAdminOrTA } from '../middleware/auth.js';
import type { AuditActionTypeValue } from '../utils/enums.js';

interface AuditLogQueryParams {
  limit?: string;
  offset?: string;
  userId?: string;
  action?: string;
  entityType?: string;
}

const VALID_ACTIONS = [
  'CREATE',
  'UPDATE',
  'DELETE',
  'LOGIN',
  'LOGOUT',
  'REQUEST_STATUS_CHANGE',
  'INVENTORY_ADJUST',
];

function parseIntParam(value: string | undefined, defaultValue: number, min = 0): number {
  if (!value) return defaultValue;
  const parsed = parseInt(value, 10);
  if (isNaN(parsed) || parsed < min) return defaultValue;
  return parsed;
}

async function handleGetAuditLogs(
  app: { log: { error: (err: unknown) => void } },
  req: FastifyRequest,
  reply: FastifyReply,
) {
  const query = req.query as AuditLogQueryParams;
  const limit = Math.min(parseIntParam(query.limit, 50, 1), 200);
  const offset = parseIntParam(query.offset, 0, 0);

  const conditions: SQL[] = [];

  if (query.userId) {
    conditions.push(eq(auditLog.userId, query.userId));
  }

  if (query.action && VALID_ACTIONS.includes(query.action)) {
    conditions.push(eq(auditLog.action, query.action as AuditActionTypeValue));
  }

  if (query.entityType) {
    conditions.push(eq(auditLog.entityType, query.entityType));
  }

  try {
    const whereClause = conditions.length > 0 ? and(...conditions) : undefined;

    const [logs, countResult] = await Promise.all([
      db
        .select({
          id: auditLog.id,
          userId: auditLog.userId,
          action: auditLog.action,
          entityType: auditLog.entityType,
          entityId: auditLog.entityId,
          oldValues: auditLog.oldValues,
          newValues: auditLog.newValues,
          ipAddress: auditLog.ipAddress,
          userAgent: auditLog.userAgent,
          metadata: auditLog.metadata,
          createdAt: auditLog.createdAt,
          userName: user.name,
          userEmail: user.email,
        })
        .from(auditLog)
        .leftJoin(user, eq(auditLog.userId, user.id))
        .where(whereClause)
        .orderBy(desc(auditLog.createdAt))
        .limit(limit)
        .offset(offset),

      db
        .select({ count: sql<number>`count(*)::int` })
        .from(auditLog)
        .where(whereClause),
    ]);

    const total = countResult[0]?.count ?? 0;

    return reply.send({
      logs,
      pagination: {
        total,
        limit,
        offset,
        hasMore: offset + limit < total,
      },
    });
  } catch (err) {
    app.log.error(err);
    return reply.code(500).send({ error: 'failed to fetch audit logs' });
  }
}

const adminRoutes: FastifyPluginCallback = (app, _opts, done) => {
  app.get('/audit-logs', { preHandler: requireAdminOrTA }, (req, reply) =>
    handleGetAuditLogs(app, req, reply),
  );

  done();
};

export default adminRoutes;
