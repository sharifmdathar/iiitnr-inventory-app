import type { FastifyPluginCallback, FastifyRequest, FastifyReply } from 'fastify';
import { eq, desc, sql, and, type SQL, ilike } from 'drizzle-orm';
import { db } from '../drizzle/db.js';
import { auditLog, user } from '../drizzle/schema.js';
import { requireAdminOrTA, requireAdmin } from '../middleware/auth.js';
import type { AuditActionTypeValue, UserRoleValue } from '../utils/enums.js';
import { userRoleValues, AuditActionType } from '../utils/enums.js';

interface AuditLogQueryParams {
  limit?: string;
  offset?: string;
  userId?: string;
  action?: string;
  entityType?: string;
}

interface UserQueryParams {
  limit?: string;
  offset?: string;
  search?: string;
}

interface UpdateUserBody {
  name?: string;
  role?: string;
  batch?: string;
  branch?: string;
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

const userPublicColumns = {
  id: user.id,
  email: user.email,
  name: user.name,
  imageUrl: user.imageUrl,
  role: user.role,
  batch: user.batch,
  branch: user.branch,
  createdAt: user.createdAt,
  updatedAt: user.updatedAt,
} as const;

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

async function handleGetUsers(
  app: { log: { error: (err: unknown) => void } },
  req: FastifyRequest,
  reply: FastifyReply,
) {
  const query = req.query as UserQueryParams;
  const limit = Math.min(parseIntParam(query.limit, 50, 1), 200);
  const offset = parseIntParam(query.offset, 0, 0);
  const search = query.search?.trim();

  const conditions: SQL[] = [];

  if (search) {
    const searchPattern = `%${search}%`;
    conditions.push(
      sql`(${ilike(user.name, searchPattern)} OR ${ilike(user.email, searchPattern)})`,
    );
  }

  try {
    const whereClause = conditions.length > 0 ? and(...conditions) : undefined;

    const [users, countResult] = await Promise.all([
      db
        .select(userPublicColumns)
        .from(user)
        .where(whereClause)
        .orderBy(desc(user.createdAt))
        .limit(limit)
        .offset(offset),

      db
        .select({ count: sql<number>`count(*)::int` })
        .from(user)
        .where(whereClause),
    ]);

    const total = countResult[0]?.count ?? 0;

    return reply.send({
      users,
      pagination: {
        total,
        limit,
        offset,
        hasMore: offset + limit < total,
      },
    });
  } catch (err) {
    app.log.error(err);
    return reply.code(500).send({ error: 'failed to fetch users' });
  }
}

async function handleUpdateUser(
  app: { log: { error: (err: unknown) => void } },
  req: FastifyRequest,
  reply: FastifyReply,
) {
  const params = req.params as { id?: string };
  const userId = params?.id;

  if (!userId) {
    return reply.code(400).send({ error: 'user id is required' });
  }

  const body = req.body as UpdateUserBody;

  try {
    const existing = await db.query.user.findFirst({
      columns: {
        id: true,
        email: true,
        name: true,
        imageUrl: true,
        role: true,
        batch: true,
        branch: true,
        createdAt: true,
        updatedAt: true,
      },
      where: (u, { eq }) => eq(u.id, userId),
    });

    if (!existing) {
      return reply.code(404).send({ error: 'user not found' });
    }

    if (body.role !== undefined) {
      if (!userRoleValues.includes(body.role as UserRoleValue)) {
        return reply.code(400).send({ error: 'invalid role' });
      }
    }

    const updateData: Record<string, unknown> = {
      updatedAt: new Date().toISOString(),
    };

    if (body.name !== undefined) updateData.name = body.name.trim() || null;
    if (body.role !== undefined) updateData.role = body.role as UserRoleValue;
    if (body.batch !== undefined) updateData.batch = body.batch.trim() || null;
    if (body.branch !== undefined) updateData.branch = body.branch.trim() || null;

    const [updated] = await db
      .update(user)
      .set(updateData)
      .where(eq(user.id, userId))
      .returning(userPublicColumns);

    if (!updated) {
      return reply.code(500).send({ error: 'failed to update user' });
    }

    await logUserUpdateAudit(req, userId, existing, updated);

    return reply.send({ user: updated });
  } catch (err) {
    app.log.error(err);
    return reply.code(500).send({ error: 'failed to update user' });
  }
}

async function logUserUpdateAudit(
  req: FastifyRequest,
  targetUserId: string,
  oldValues: Record<string, unknown>,
  newValues: Record<string, unknown>,
) {
  try {
    const actorId = (req.user as { sub?: string })?.sub;
    await db.insert(auditLog).values({
      id: crypto.randomUUID(),
      userId: actorId,
      action: AuditActionType.UPDATE,
      entityType: 'User',
      entityId: targetUserId,
      oldValues: JSON.stringify(oldValues),
      newValues: JSON.stringify(newValues),
      ipAddress: req.ip,
      userAgent: req.headers['user-agent'],
      createdAt: new Date().toISOString(),
    });
  } catch (err) {
    console.error('Failed to write audit log for user update:', err);
  }
}

const adminRoutes: FastifyPluginCallback = (app, _opts, done) => {
  app.get('/audit-logs', { preHandler: requireAdminOrTA }, (req, reply) =>
    handleGetAuditLogs(app, req, reply),
  );

  app.get('/users', { preHandler: requireAdmin }, (req, reply) => handleGetUsers(app, req, reply));

  app.patch('/users/:id', { preHandler: requireAdmin }, (req, reply) =>
    handleUpdateUser(app, req, reply),
  );

  done();
};

export default adminRoutes;
