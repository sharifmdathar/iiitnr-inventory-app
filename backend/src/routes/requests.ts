import type { FastifyPluginAsync, FastifyRequest, FastifyReply } from 'fastify';
import { and, desc, eq, inArray, sql } from 'drizzle-orm';
import { db } from '../drizzle/db.js';
import { component, request, requestItem, user } from '../drizzle/schema.js';
import { requireAuth, isAdminOrTA } from '../middleware/auth.js';
import { RequestStatus, requestStatusValues, UserRole, AuditActionType } from '../utils/enums.js';
import type { RequestStatusValue, UserRoleValue } from '../utils/enums.js';
import { logAudit, getUserIdFromRequest } from '../utils/audit.js';
interface CurrentUser {
  sub?: string;
  role?: UserRoleValue;
}

interface RequestItemInput {
  componentId?: string;
  quantity?: number;
}

interface CreateRequestBody {
  items?: RequestItemInput[];
  targetFacultyId?: string;
  projectTitle?: string;
}

interface UpdateStatusBody {
  status?: string;
}

interface RequestQuery {
  userId?: string;
  status?: string;
}

interface NormalizedItem {
  componentId: string;
  quantity: number;
}

interface LockedComponent {
  id: string;
  name: string;
  availableQuantity: number;
  totalQuantity: number;
}

type RequestWithRelations = Awaited<ReturnType<typeof fetchRequestWithItems>>;
type FullRequest = Awaited<ReturnType<typeof fetchFullRequest>>;

const createRequestSchema = {
  body: {
    type: 'object',
    properties: {
      items: {
        type: 'array',
        minItems: 1,
        maxItems: 100,
        items: {
          type: 'object',
          properties: {
            componentId: { type: 'string', maxLength: 100 },
            quantity: { type: 'integer', minimum: 1 },
          },
          additionalProperties: false,
        },
      },
      targetFacultyId: { type: 'string', maxLength: 100 },
      projectTitle: { type: 'string', minLength: 1, maxLength: 300 },
    },
    additionalProperties: false,
  },
} as const;

const updateRequestStatusSchema = {
  body: {
    type: 'object',
    required: ['status'],
    properties: {
      status: { type: 'string', maxLength: 50 },
    },
    additionalProperties: false,
  },
} as const;

function getCurrentUser(req: FastifyRequest): CurrentUser {
  return req.user as CurrentUser;
}

async function fetchRequestWithItems(id: string) {
  const [row] = await db.query.request.findMany({
    where: eq(request.id, id),
    with: { requestItems: { with: { component: true } } },
  });
  return row;
}

async function fetchFullRequest(id: string) {
  const [row] = await db.query.request.findMany({
    where: eq(request.id, id),
    with: {
      requestItems: { with: { component: true } },
      user_userId: { columns: { id: true, email: true, name: true, role: true } },
      user_targetFacultyId: { columns: { id: true, email: true, name: true, role: true } },
    },
  });
  return row;
}

function shapeRequest(row: NonNullable<FullRequest>) {
  return {
    ...row,
    items: row.requestItems,
    user: row.user_userId,
    targetFaculty: row.user_targetFacultyId,
  };
}

async function fetchAndShapeRequest(id: string) {
  const row = await fetchFullRequest(id);
  return row ? shapeRequest(row) : null;
}

async function updateRequestStatus(id: string, status: RequestStatusValue) {
  await db
    .update(request)
    .set({ status, updatedAt: new Date().toISOString() })
    .where(eq(request.id, id));
}

async function validateFacultyExists(facultyId: string): Promise<boolean> {
  const facultyRow = await db.query.user.findFirst({
    columns: { id: true },
    where: (u, { eq, and }) => and(eq(u.id, facultyId), eq(u.role, UserRole.FACULTY)),
  });
  return !!facultyRow;
}

async function validateComponentsExist(componentIds: string[]): Promise<boolean> {
  const existingComponents = await db
    .select({ id: component.id })
    .from(component)
    .where(inArray(component.id, componentIds));
  return existingComponents.length === componentIds.length;
}

interface ValidationError {
  code: number;
  message: string;
}

function validateCreateRequestBody(body: CreateRequestBody): ValidationError | null {
  const items = body?.items ?? [];
  const targetFacultyId = body?.targetFacultyId?.trim();
  const projectTitle = body?.projectTitle?.trim();

  if (!Array.isArray(items) || items.length === 0) {
    return { code: 400, message: 'items are required' };
  }
  if (!targetFacultyId) {
    return { code: 400, message: 'targetFacultyId is required' };
  }
  if (!projectTitle) {
    return { code: 400, message: 'projectTitle is required' };
  }
  return null;
}

function normalizeAndValidateItems(items: RequestItemInput[]): NormalizedItem[] | ValidationError {
  const normalized: NormalizedItem[] = [];

  for (const item of items) {
    const componentId = item?.componentId?.trim();
    const quantity = item?.quantity;

    if (!componentId) {
      return { code: 400, message: 'componentId is required' };
    }
    if (typeof quantity !== 'number' || quantity <= 0) {
      return { code: 400, message: 'quantity must be a positive number' };
    }
    normalized.push({ componentId, quantity });
  }

  const componentIds = normalized.map((item) => item.componentId);
  if (new Set(componentIds).size !== componentIds.length) {
    return { code: 400, message: 'duplicate componentId in request' };
  }

  return normalized;
}

function validateStatusInput(status: string | undefined): ValidationError | null {
  const trimmed = status?.trim();

  if (!trimmed) {
    return { code: 400, message: 'status is required' };
  }
  if (!requestStatusValues.includes(trimmed as RequestStatusValue)) {
    return { code: 400, message: 'invalid status' };
  }

  return null;
}

function isValidationError(value: unknown): value is ValidationError {
  return typeof value === 'object' && value !== null && 'code' in value && 'message' in value;
}

function canApproveOrRejectRequest(
  currentUser: CurrentUser,
  existingRequest: NonNullable<RequestWithRelations>,
): ValidationError | null {
  if (currentUser.role === UserRole.FACULTY) {
    if (existingRequest.targetFacultyId !== currentUser.sub) {
      return { code: 403, message: 'forbidden: can only approve/reject requests targeting you' };
    }
    return null;
  }

  if (!isAdminOrTA(currentUser.role)) {
    return {
      code: 403,
      message: 'forbidden: only faculty, admin, or TA can approve/reject requests',
    };
  }

  return null;
}

function canFulfillRequest(currentUser: CurrentUser): ValidationError | null {
  if (!isAdminOrTA(currentUser.role)) {
    return { code: 403, message: 'forbidden: only admin or TA can fulfill requests' };
  }
  return null;
}

function canDeleteRequest(
  currentUser: CurrentUser,
  existingRequest: { userId: string; status: string },
): ValidationError | null {
  const isOwner = existingRequest.userId === currentUser.sub;
  const isPrivileged = isAdminOrTA(currentUser.role);

  if (!isOwner && !isPrivileged) {
    return { code: 403, message: 'forbidden: cannot delete this request' };
  }
  if (existingRequest.status !== RequestStatus.PENDING) {
    return { code: 400, message: 'request can only be deleted when status is PENDING' };
  }
  return null;
}

async function fulfillRequestTransaction(existingRequest: NonNullable<RequestWithRelations>) {
  const fulfilledAt = new Date().toISOString();
  await db.transaction(async (tx) => {
    for (const item of existingRequest.requestItems) {
      const lockResult = await tx.execute(
        sql`SELECT "id", "name", "availableQuantity", "totalQuantity" FROM "Component" WHERE "id" = ${item.componentId} FOR UPDATE`,
      );
      const lockedComp = lockResult.rows[0] as LockedComponent | undefined;

      if (!lockedComp || lockedComp.availableQuantity < item.quantity) {
        const name = lockedComp?.name ?? item.component?.name ?? 'unknown';
        throw new Error(`INSUFFICIENT_QUANTITY:${name}:${item.quantity}`);
      }

      await tx
        .update(component)
        .set({
          availableQuantity: lockedComp.availableQuantity - item.quantity,
          updatedAt: fulfilledAt,
        })
        .where(eq(component.id, item.componentId));
    }

    await tx
      .update(request)
      .set({
        status: RequestStatus.FULFILLED,
        updatedAt: fulfilledAt,
        fulfilledAt,
      })
      .where(eq(request.id, existingRequest.id));
  });
}

async function returnRequestTransaction(existingRequest: NonNullable<RequestWithRelations>) {
  const returnedAt = new Date().toISOString();
  await db.transaction(async (tx) => {
    for (const item of existingRequest.requestItems) {
      const lockResult = await tx.execute(
        sql`SELECT "id", "name", "availableQuantity", "totalQuantity" FROM "Component" WHERE "id" = ${item.componentId} FOR UPDATE`,
      );
      const lockedComp = lockResult.rows[0] as LockedComponent | undefined;

      if (!lockedComp) {
        const name = item.component?.name ?? 'unknown';
        throw new Error(`COMPONENT_NOT_FOUND:${name}`);
      }

      const nextAvailable = lockedComp.availableQuantity + item.quantity;
      const nextTotal = Math.max(lockedComp.totalQuantity, nextAvailable);

      await tx
        .update(component)
        .set({
          availableQuantity: nextAvailable,
          totalQuantity: nextTotal,
          updatedAt: returnedAt,
        })
        .where(eq(component.id, item.componentId));
    }

    await tx
      .update(request)
      .set({
        status: RequestStatus.RETURNED,
        updatedAt: returnedAt,
        returnedAt,
      })
      .where(eq(request.id, existingRequest.id));
  });
}

function parseInsufficientQuantityError(error: Error): string | null {
  if (error.message.startsWith('INSUFFICIENT_QUANTITY:')) {
    return error.message.split(':')[1] ?? 'unknown';
  }
  return null;
}

async function handleCreateRequest(
  app: { log: { error: (err: unknown) => void } },
  req: FastifyRequest,
  reply: FastifyReply,
) {
  const body = req.body as CreateRequestBody;
  const userId = getCurrentUser(req)?.sub;
  if (!userId) return;

  const bodyError = validateCreateRequestBody(body);
  if (bodyError) {
    return reply.code(bodyError.code).send({ error: bodyError.message });
  }

  const targetFacultyId = body.targetFacultyId!.trim();
  const projectTitle = body.projectTitle!.trim();

  const itemsResult = normalizeAndValidateItems(body.items!);
  if (isValidationError(itemsResult)) {
    return reply.code(itemsResult.code).send({ error: itemsResult.message });
  }
  const normalizedItems = itemsResult;

  try {
    if (!(await validateFacultyExists(targetFacultyId))) {
      return reply.code(400).send({ error: 'invalid targetFacultyId' });
    }

    const componentIds = normalizedItems.map((item) => item.componentId);
    if (!(await validateComponentsExist(componentIds))) {
      return reply.code(400).send({ error: 'one or more components not found' });
    }

    const now = new Date().toISOString();
    const requestId = crypto.randomUUID();

    await db.insert(request).values({
      id: requestId,
      userId,
      targetFacultyId,
      projectTitle,
      status: RequestStatus.PENDING,
      createdAt: now,
      updatedAt: now,
    });

    await db.insert(requestItem).values(
      normalizedItems.map((item) => ({
        id: crypto.randomUUID(),
        requestId,
        componentId: item.componentId,
        quantity: item.quantity,
        createdAt: now,
        updatedAt: now,
      })),
    );

    const createdRequest = await fetchAndShapeRequest(requestId);
    if (!createdRequest) {
      return reply.code(500).send({ error: 'failed to create request' });
    }

    await logAudit(
      {
        userId: getUserIdFromRequest(req),
        action: AuditActionType.CREATE,
        entityType: 'Request',
        entityId: requestId,
        newValues: createdRequest as Record<string, unknown>,
      },
      req,
    );

    return reply.code(201).send({ request: createdRequest });
  } catch (err) {
    app.log.error(err);
    return reply.code(500).send({ error: 'failed to create request' });
  }
}

async function handleGetFaculty(
  app: { log: { error: (err: unknown) => void } },
  reply: FastifyReply,
) {
  try {
    const faculty = await db
      .select({ id: user.id, email: user.email, name: user.name, role: user.role })
      .from(user)
      .where(eq(user.role, UserRole.FACULTY))
      .orderBy(desc(user.createdAt));

    return reply.send({ faculty });
  } catch (err) {
    app.log.error(err);
    return reply.code(500).send({ error: 'failed to fetch faculty' });
  }
}

async function handleGetRequests(
  app: { log: { error: (err: unknown) => void } },
  req: FastifyRequest,
  reply: FastifyReply,
) {
  const currentUser = getCurrentUser(req);
  const currentUserId = currentUser?.sub;
  if (!currentUserId) return;

  const query = req.query as RequestQuery;
  const requestedUserId = query?.userId?.trim();
  const status = query?.status?.trim();

  if (status && !requestStatusValues.includes(status as RequestStatusValue)) {
    return reply.code(400).send({ error: 'invalid status' });
  }

  const conditions = [];

  if (status) {
    conditions.push(eq(request.status, status as RequestStatusValue));
  }

  if (currentUser.role === UserRole.FACULTY) {
    conditions.push(eq(request.targetFacultyId, currentUserId));
  } else if (isAdminOrTA(currentUser.role)) {
    if (requestedUserId) {
      conditions.push(eq(request.userId, requestedUserId));
    }
  } else {
    conditions.push(eq(request.userId, currentUserId));
  }

  try {
    const whereClause = conditions.length > 0 ? and(...conditions) : undefined;

    const rows = await db.query.request.findMany({
      where: whereClause,
      orderBy: desc(request.createdAt),
      with: {
        requestItems: { with: { component: true } },
        user_userId: { columns: { id: true, email: true, name: true, role: true } },
        user_targetFacultyId: { columns: { id: true, email: true, name: true, role: true } },
      },
    });

    const requests = rows.map(shapeRequest);
    return reply.send({ requests });
  } catch (err) {
    app.log.error(err);
    return reply.code(500).send({ error: 'failed to fetch requests' });
  }
}

async function handlePendingStatusUpdate(
  req: FastifyRequest,
  reply: FastifyReply,
  existingRequest: NonNullable<RequestWithRelations>,
  newStatus: RequestStatusValue,
  currentUser: CurrentUser,
) {
  const allowedPendingTransitions: RequestStatusValue[] = [
    RequestStatus.REJECTED,
    RequestStatus.APPROVED,
  ];

  if (!allowedPendingTransitions.includes(newStatus)) {
    return reply.code(400).send({ error: 'PENDING requests can only be APPROVED or REJECTED' });
  }

  const authError = canApproveOrRejectRequest(currentUser, existingRequest);
  if (authError) {
    return reply.code(authError.code).send({ error: authError.message });
  }

  await updateRequestStatus(existingRequest.id, newStatus);
  const updatedRequest = await fetchAndShapeRequest(existingRequest.id);

  await logAudit(
    {
      userId: getUserIdFromRequest(req),
      action: AuditActionType.REQUEST_STATUS_CHANGE,
      entityType: 'Request',
      entityId: existingRequest.id,
      oldValues: { status: existingRequest.status },
      newValues: { status: newStatus },
    },
    req,
  );

  return reply.send({ request: updatedRequest });
}

async function handleApprovedStatusUpdate(
  req: FastifyRequest,
  reply: FastifyReply,
  existingRequest: NonNullable<RequestWithRelations>,
  newStatus: RequestStatusValue,
  currentUser: CurrentUser,
) {
  if (newStatus !== RequestStatus.FULFILLED) {
    return reply.code(400).send({ error: 'approved request can only be set to FULFILLED' });
  }

  const authError = canFulfillRequest(currentUser);
  if (authError) {
    return reply.code(authError.code).send({ error: authError.message });
  }

  try {
    await fulfillRequestTransaction(existingRequest);
    const updatedRequest = await fetchAndShapeRequest(existingRequest.id);

    await logAudit(
      {
        userId: getUserIdFromRequest(req),
        action: AuditActionType.REQUEST_STATUS_CHANGE,
        entityType: 'Request',
        entityId: existingRequest.id,
        oldValues: { status: existingRequest.status },
        newValues: { status: newStatus, fulfilledItems: existingRequest.requestItems },
      },
      req,
    );

    return reply.send({ request: updatedRequest });
  } catch (error) {
    if (error instanceof Error) {
      const componentName = parseInsufficientQuantityError(error);
      if (componentName) {
        return reply.code(400).send({
          error: `insufficient quantity for component "${componentName}"`,
        });
      }
    }
    throw error;
  }
}

async function handleFulfilledStatusUpdate(
  req: FastifyRequest,
  reply: FastifyReply,
  existingRequest: NonNullable<RequestWithRelations>,
  newStatus: RequestStatusValue,
  currentUser: CurrentUser,
) {
  if (newStatus !== RequestStatus.RETURNED) {
    return reply.code(400).send({ error: 'fulfilled request can only be set to RETURNED' });
  }

  const authError = canFulfillRequest(currentUser);
  if (authError) {
    return reply.code(authError.code).send({ error: authError.message });
  }

  await returnRequestTransaction(existingRequest);
  const updatedRequest = await fetchAndShapeRequest(existingRequest.id);

  await logAudit(
    {
      userId: getUserIdFromRequest(req),
      action: AuditActionType.REQUEST_STATUS_CHANGE,
      entityType: 'Request',
      entityId: existingRequest.id,
      oldValues: { status: existingRequest.status },
      newValues: { status: newStatus, returnedItems: existingRequest.requestItems },
    },
    req,
  );

  return reply.send({ request: updatedRequest });
}

async function handleUpdateRequestStatus(
  app: { log: { error: (err: unknown) => void } },
  req: FastifyRequest,
  reply: FastifyReply,
) {
  const params = req.params as { id?: string };
  const body = req.body as UpdateStatusBody;
  const id = params?.id;

  if (!id) {
    return reply.code(400).send({ error: 'request id is required' });
  }

  const statusError = validateStatusInput(body?.status);
  if (statusError) {
    return reply.code(statusError.code).send({ error: statusError.message });
  }

  const newStatus = body!.status!.trim() as RequestStatusValue;
  const currentUser = getCurrentUser(req);
  const currentUserId = currentUser?.sub;
  if (!currentUserId) return;

  try {
    const existingRequest = await fetchRequestWithItems(id);

    if (!existingRequest) {
      return reply.code(404).send({ error: 'request not found' });
    }

    switch (existingRequest.status) {
      case RequestStatus.PENDING:
        return await handlePendingStatusUpdate(req, reply, existingRequest, newStatus, currentUser);

      case RequestStatus.APPROVED:
        return await handleApprovedStatusUpdate(
          req,
          reply,
          existingRequest,
          newStatus,
          currentUser,
        );

      case RequestStatus.FULFILLED:
        return await handleFulfilledStatusUpdate(
          req,
          reply,
          existingRequest,
          newStatus,
          currentUser,
        );

      default:
        return reply.code(400).send({
          error:
            'request status can only be updated when status is PENDING, APPROVED, or FULFILLED',
        });
    }
  } catch (err) {
    app.log.error(err);
    return reply.code(500).send({ error: 'failed to update request' });
  }
}

async function handleDeleteRequest(
  app: { log: { error: (err: unknown) => void } },
  req: FastifyRequest,
  reply: FastifyReply,
) {
  const params = req.params as { id?: string };
  const id = params?.id;

  if (!id) {
    return reply.code(400).send({ error: 'request id is required' });
  }

  const currentUser = getCurrentUser(req);
  const currentUserId = currentUser?.sub;
  if (!currentUserId) return;

  try {
    const existingRequest = await db.query.request.findFirst({
      where: (r, { eq }) => eq(r.id, id),
    });

    if (!existingRequest) {
      return reply.code(404).send({ error: 'request not found' });
    }

    const authError = canDeleteRequest(currentUser, existingRequest);
    if (authError) {
      return reply.code(authError.code).send({ error: authError.message });
    }

    await db.delete(requestItem).where(eq(requestItem.requestId, id));
    await db.delete(request).where(eq(request.id, id));

    await logAudit(
      {
        userId: getUserIdFromRequest(req),
        action: AuditActionType.DELETE,
        entityType: 'Request',
        entityId: id,
        oldValues: { status: existingRequest.status, userId: existingRequest.userId },
      },
      req,
    );

    return reply.code(204).send();
  } catch (err) {
    app.log.error(err);
    return reply.code(500).send({ error: 'failed to delete request' });
  }
}

const requestsRoutes: FastifyPluginAsync = async (app) => {
  app.post('/requests', { preHandler: requireAuth, schema: createRequestSchema }, (req, reply) =>
    handleCreateRequest(app, req, reply),
  );

  app.get('/faculty', { preHandler: requireAuth }, (_, reply) => handleGetFaculty(app, reply));

  app.get('/requests', { preHandler: requireAuth }, (req, reply) =>
    handleGetRequests(app, req, reply),
  );

  app.put(
    '/requests/:id',
    { preHandler: requireAuth, schema: updateRequestStatusSchema },
    (req, reply) => handleUpdateRequestStatus(app, req, reply),
  );

  app.delete('/requests/:id', { preHandler: requireAuth }, (req, reply) =>
    handleDeleteRequest(app, req, reply),
  );
};

export default requestsRoutes;
