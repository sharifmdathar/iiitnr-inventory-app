import type { FastifyPluginCallback, FastifyRequest, FastifyReply } from 'fastify';
import { and, desc, eq, inArray } from 'drizzle-orm';
import { db } from '../drizzle/db.js';
import { component, request, requestItem, user } from '../drizzle/schema.js';
import { requireAuth, isAdminOrLA } from '../middleware/auth.js';
import { RequestStatus, requestStatusValues, UserRole, AuditActionType } from '../utils/enums.js';
import type { RequestStatusValue, UserRoleValue } from '../utils/enums.js';
import { logAudit, getUserIdFromRequest } from '../utils/audit.js';
import { expireOverdueRequests, REQUEST_RETURN_LIMIT_MS } from '../services/request-expiry.js';
import { events, EventType, notifyRequestsUpdated } from '../utils/events.js';
import type { EventMessage } from 'fastify-sse-v2';
import { pushable } from 'it-pushable';
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

interface RequestQuery {
  userId?: string;
  status?: string;
}

interface NormalizedItem {
  componentId: string;
  quantity: number;
}

interface IssueItemInput {
  componentId: string;
  quantity: number;
}

interface ReturnItemInput {
  componentId: string;
  quantity: number;
}

interface UpdateStatusBody {
  status?: string;
  lastRenewReason?: string;
  issueItems?: IssueItemInput[];
  returnItems?: ReturnItemInput[];
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
      lastRenewReason: { type: 'string', maxLength: 500 },
      issueItems: {
        type: 'array',
        items: {
          type: 'object',
          properties: {
            componentId: { type: 'string', maxLength: 100 },
            quantity: { type: 'integer', minimum: 1 },
          },
          required: ['componentId', 'quantity'],
          additionalProperties: false,
        },
      },
      returnItems: {
        type: 'array',
        items: {
          type: 'object',
          properties: {
            componentId: { type: 'string', maxLength: 100 },
            quantity: { type: 'integer', minimum: 1 },
          },
          required: ['componentId', 'quantity'],
          additionalProperties: false,
        },
      },
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
      user_userId: {
        columns: { id: true, email: true, name: true, role: true, batch: true, branch: true },
      },
      user_targetFacultyId: {
        columns: { id: true, email: true, name: true, role: true, batch: true, branch: true },
      },
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
  notifyRequestsUpdated();
}

async function validateFacultyExists(facultyId: string): Promise<boolean> {
  const facultyRow = await db.query.user.findFirst({
    columns: { id: true },
    where: (u, { eq, and }) => and(eq(u.id, facultyId), eq(u.role, UserRole.FACULTY)),
  });
  return Boolean(facultyRow);
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

function canRenewApproveOrRejectRequest(
  currentUser: CurrentUser,
  existingRequest: NonNullable<RequestWithRelations>,
): ValidationError | null {
  if (currentUser.role === UserRole.FACULTY) {
    if (existingRequest.targetFacultyId !== currentUser.sub) {
      return { code: 403, message: 'forbidden: can only approve/reject requests targeting you' };
    }
    return null;
  }

  if (!isAdminOrLA(currentUser.role)) {
    return {
      code: 403,
      message: 'forbidden: only faculty, admin, or LA can approve/reject requests',
    };
  }

  return null;
}

function canIssueRequest(currentUser: CurrentUser): ValidationError | null {
  if (!isAdminOrLA(currentUser.role)) {
    return { code: 403, message: 'forbidden: only admin or LA can issue requests' };
  }
  return null;
}

function canDeleteRequest(
  currentUser: CurrentUser,
  existingRequest: { userId: string; status: string },
): ValidationError | null {
  const isOwner = existingRequest.userId === currentUser.sub;
  const isPrivileged = isAdminOrLA(currentUser.role);

  if (!isOwner && !isPrivileged) {
    return { code: 403, message: 'forbidden: cannot delete this request' };
  }
  if (existingRequest.status !== RequestStatus.PENDING) {
    return { code: 400, message: 'request can only be deleted when status is PENDING' };
  }
  return null;
}

async function issueRequestTransaction(
  existingRequest: NonNullable<RequestWithRelations>,
  issueItems?: IssueItemInput[],
) {
  const fulfilledAt = new Date().toISOString();
  await db.transaction(async (tx) => {
    const requestItems = existingRequest.requestItems;
    let hasRemainingToFulfill = false;

    for (const item of requestItems) {
      const requestedQty = item.quantity;
      const alreadyFulfilled = item.fulfilledQuantity ?? 0;
      const remainingToFulfill = requestedQty - alreadyFulfilled;

      if (remainingToFulfill <= 0) {
        continue;
      }

      const issueItem = issueItems?.find((i) => i.componentId === item.componentId);
      const issueQty = issueItem
        ? Math.min(issueItem.quantity, remainingToFulfill)
        : issueItems
          ? 0
          : remainingToFulfill;

      if (issueQty <= 0) {
        hasRemainingToFulfill = true;
        continue;
      }

      const [lockedComp] = await tx
        .select({
          id: component.id,
          name: component.name,
          availableQuantity: component.availableQuantity,
          totalQuantity: component.totalQuantity,
        })
        .from(component)
        .where(eq(component.id, item.componentId))
        .for('update');

      if (!lockedComp || lockedComp.availableQuantity < issueQty) {
        const name = lockedComp?.name ?? item.component?.name ?? 'unknown';
        throw new Error(`INSUFFICIENT_QUANTITY:${name}:${issueQty}`);
      }

      const newFulfilled = alreadyFulfilled + issueQty;
      await tx
        .update(requestItem)
        .set({
          fulfilledQuantity: newFulfilled,
          updatedAt: fulfilledAt,
        })
        .where(eq(requestItem.id, item.id));

      await tx
        .update(component)
        .set({
          availableQuantity: lockedComp.availableQuantity - issueQty,
          updatedAt: fulfilledAt,
        })
        .where(eq(component.id, item.componentId));

      if (newFulfilled < requestedQty) {
        hasRemainingToFulfill = true;
      }
    }

    const returnDueAt = new Date(Date.now() + REQUEST_RETURN_LIMIT_MS).toISOString();
    const newStatus = hasRemainingToFulfill ? RequestStatus.PARTIALLY_ISSUED : RequestStatus.ISSUED;

    await tx
      .update(request)
      .set({
        status: newStatus,
        updatedAt: fulfilledAt,
        fulfilledAt,
        returnDueAt,
      })
      .where(eq(request.id, existingRequest.id));
    notifyRequestsUpdated();
  });
}

async function requestForRenewalTransaction(
  existingRequest: NonNullable<RequestWithRelations>,
  lastRenewReason: string | undefined,
) {
  const requestedRenewalAt = new Date().toISOString();
  await db.transaction(async (tx) => {
    await tx
      .update(request)
      .set({
        status: RequestStatus.REQUESTED_RENEW,
        updatedAt: requestedRenewalAt,
        lastRenewReason,
      })
      .where(eq(request.id, existingRequest.id));
    notifyRequestsUpdated();
  });
}

async function approveRenewRequestTransaction(existingRequest: NonNullable<RequestWithRelations>) {
  const renewedAt = new Date().toISOString();
  const newReturnDueAt = new Date(Date.now() + REQUEST_RETURN_LIMIT_MS).toISOString();

  await db.transaction(async (tx) => {
    await tx
      .update(request)
      .set({
        status: RequestStatus.RENEWED,
        updatedAt: renewedAt,
        returnDueAt: newReturnDueAt,
        lastRenewDate: renewedAt,
      })
      .where(eq(request.id, existingRequest.id));
    notifyRequestsUpdated();
  });
}

async function returnRequestTransaction(
  existingRequest: NonNullable<RequestWithRelations>,
  returnItems?: ReturnItemInput[],
) {
  const returnedAt = new Date().toISOString();
  await db.transaction(async (tx) => {
    let hasIssuedRemaining = false;
    let hasAnyReturned = false;

    for (const item of existingRequest.requestItems) {
      const issuedQty = item.fulfilledQuantity ?? 0;
      const alreadyReturnedQty = item.returnedQuantity ?? 0;
      const currentlyHeldQty = issuedQty - alreadyReturnedQty;

      if (currentlyHeldQty <= 0) {
        continue;
      }

      const returnItem = returnItems?.find((i) => i.componentId === item.componentId);
      const returnQty = returnItem
        ? Math.min(returnItem.quantity, currentlyHeldQty)
        : returnItems
          ? 0
          : currentlyHeldQty;

      if (returnQty <= 0) {
        hasIssuedRemaining = true;
        continue;
      }

      hasAnyReturned = true;

      const [lockedComp] = await tx
        .select({
          id: component.id,
          name: component.name,
          availableQuantity: component.availableQuantity,
          totalQuantity: component.totalQuantity,
        })
        .from(component)
        .where(eq(component.id, item.componentId))
        .for('update');

      if (!lockedComp) {
        const name = item.component?.name ?? 'unknown';
        throw new Error(`COMPONENT_NOT_FOUND:${name}`);
      }

      const newReturned = alreadyReturnedQty + returnQty;
      await tx
        .update(requestItem)
        .set({
          returnedQuantity: newReturned,
          updatedAt: returnedAt,
        })
        .where(eq(requestItem.id, item.id));

      const nextAvailable = lockedComp.availableQuantity + returnQty;
      const nextTotal = Math.max(lockedComp.totalQuantity, nextAvailable);

      await tx
        .update(component)
        .set({
          availableQuantity: nextAvailable,
          totalQuantity: nextTotal,
          updatedAt: returnedAt,
        })
        .where(eq(component.id, item.componentId));

      if (issuedQty - newReturned > 0) {
        hasIssuedRemaining = true;
      }
    }

    const newStatus =
      hasIssuedRemaining && hasAnyReturned
        ? RequestStatus.PARTIALLY_RETURNED
        : RequestStatus.RETURNED;

    await tx
      .update(request)
      .set({
        status: newStatus,
        updatedAt: returnedAt,
        returnedAt: newStatus === RequestStatus.RETURNED ? returnedAt : null,
      })
      .where(eq(request.id, existingRequest.id));
    notifyRequestsUpdated();
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
  if (!userId) {
    reply.code(401).send({ error: 'unauthorized' });
    return;
  }

  const bodyError = validateCreateRequestBody(body);
  if (bodyError) {
    reply.code(bodyError.code).send({ error: bodyError.message });
    return;
  }

  const targetFacultyId = body.targetFacultyId?.trim();
  const projectTitle = body.projectTitle?.trim();
  const rawItems = body.items;
  if (!targetFacultyId || !projectTitle || !Array.isArray(rawItems)) {
    reply.code(400).send({ error: 'invalid request body' });
    return;
  }

  const itemsResult = normalizeAndValidateItems(rawItems);
  if (isValidationError(itemsResult)) {
    reply.code(itemsResult.code).send({ error: itemsResult.message });
    return;
  }
  const normalizedItems = itemsResult;

  try {
    if (!(await validateFacultyExists(targetFacultyId))) {
      reply.code(400).send({ error: 'invalid targetFacultyId' });
      return;
    }

    const componentIds = normalizedItems.map((item) => item.componentId);
    if (!(await validateComponentsExist(componentIds))) {
      reply.code(400).send({ error: 'one or more components not found' });
      return;
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
      reply.code(500).send({ error: 'failed to create request' });
      return;
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

    reply.code(201).send({ request: createdRequest });
    notifyRequestsUpdated();
  } catch (err) {
    app.log.error(err);
    reply.code(500).send({ error: 'failed to create request' });
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

    reply.send({ faculty });
  } catch (err) {
    app.log.error(err);
    reply.code(500).send({ error: 'failed to fetch faculty' });
  }
}

async function handleGetRequests(
  app: { log: { error: (err: unknown) => void } },
  req: FastifyRequest,
  reply: FastifyReply,
) {
  const currentUser = getCurrentUser(req);
  const currentUserId = currentUser?.sub;
  if (!currentUserId) {
    reply.code(401).send({ error: 'unauthorized' });
    return;
  }

  const query = req.query as RequestQuery;
  const requestedUserId = query?.userId?.trim();
  const status = query?.status?.trim();

  if (status && !requestStatusValues.includes(status as RequestStatusValue)) {
    reply.code(400).send({ error: 'invalid status' });
    return;
  }

  const conditions = [];

  if (status) {
    conditions.push(eq(request.status, status as RequestStatusValue));
  }

  if (currentUser.role === UserRole.FACULTY) {
    conditions.push(eq(request.targetFacultyId, currentUserId));
  } else if (isAdminOrLA(currentUser.role)) {
    if (requestedUserId) {
      conditions.push(eq(request.userId, requestedUserId));
    }
  } else {
    conditions.push(eq(request.userId, currentUserId));
  }

  try {
    if (status === RequestStatus.EXPIRED) {
      await expireOverdueRequests();
    }

    const whereClause = conditions.length > 0 ? and(...conditions) : undefined;

    const rows = await db.query.request.findMany({
      where: whereClause,
      orderBy: desc(request.createdAt),
      with: {
        requestItems: { with: { component: true } },
        user_userId: {
          columns: { id: true, email: true, name: true, role: true, batch: true, branch: true },
        },
        user_targetFacultyId: {
          columns: { id: true, email: true, name: true, role: true, batch: true, branch: true },
        },
      },
    });

    const requests = rows.map(shapeRequest);
    reply.send({ requests });
  } catch (err) {
    app.log.error(err);
    reply.code(500).send({ error: 'failed to fetch requests' });
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
    reply.code(400).send({ error: 'PENDING requests can only be APPROVED or REJECTED' });
    return;
  }

  const authError = canRenewApproveOrRejectRequest(currentUser, existingRequest);
  if (authError) {
    reply.code(authError.code).send({ error: authError.message });
    return;
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

  reply.send({ request: updatedRequest });
}

async function handleApprovedStatusUpdate(
  req: FastifyRequest,
  reply: FastifyReply,
  existingRequest: NonNullable<RequestWithRelations>,
  newStatus: RequestStatusValue,
  currentUser: CurrentUser,
) {
  if (newStatus !== RequestStatus.ISSUED) {
    reply.code(400).send({ error: 'approved request can only be set to ISSUED' });
    return;
  }

  const authError = canIssueRequest(currentUser);
  if (authError) {
    reply.code(authError.code).send({ error: authError.message });
    return;
  }

  const body = req.body as UpdateStatusBody;
  const issueItems = body.issueItems;

  try {
    await issueRequestTransaction(existingRequest, issueItems);
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

    reply.send({ request: updatedRequest });
  } catch (error) {
    if (error instanceof Error) {
      const componentName = parseInsufficientQuantityError(error);
      if (componentName) {
        reply.code(400).send({
          error: `insufficient quantity for component "${componentName}"`,
        });
        return;
      }
    }
    throw error;
  }
}

async function handleRequestedRenewalStatusUpdate(
  req: FastifyRequest,
  reply: FastifyReply,
  existingRequest: NonNullable<RequestWithRelations>,
  newStatus: RequestStatusValue,
  currentUser: CurrentUser,
) {
  if (newStatus !== RequestStatus.RENEWED) {
    reply.code(400).send({ error: 'requested renewal request can only be set to RENEWED' });
    return;
  }

  const authError = canRenewApproveOrRejectRequest(currentUser, existingRequest);
  if (authError) {
    reply.code(authError.code).send({ error: authError.message });
    return;
  }
  await approveRenewRequestTransaction(existingRequest);
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
  reply.send({ request: updatedRequest });
}

async function handleIssuedStatusUpdate(
  req: FastifyRequest,
  reply: FastifyReply,
  existingRequest: NonNullable<RequestWithRelations>,
  newStatus: RequestStatusValue,
  currentUser: CurrentUser,
  lastRenewReason: string | undefined,
) {
  if (
    newStatus !== RequestStatus.RETURNED &&
    newStatus !== RequestStatus.REQUESTED_RENEW &&
    newStatus !== RequestStatus.PARTIALLY_RETURNED
  ) {
    reply.code(400).send({
      error:
        'issued request can only be set to RETURNED, PARTIALLY_RETURNED, RENEWED, or REQUESTED_RENEW',
    });
    return;
  }
  if (newStatus === RequestStatus.RETURNED || newStatus === RequestStatus.PARTIALLY_RETURNED) {
    const authError = canIssueRequest(currentUser);
    if (authError) {
      reply.code(authError.code).send({ error: authError.message });
      return;
    }
    const body = req.body as UpdateStatusBody;
    const returnItems = body.returnItems;
    await returnRequestTransaction(existingRequest, returnItems);
  } else if (newStatus === RequestStatus.REQUESTED_RENEW) {
    await requestForRenewalTransaction(existingRequest, lastRenewReason);
  }
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

  reply.send({ request: updatedRequest });
}

async function handleExpiredStatusUpdate(
  req: FastifyRequest,
  reply: FastifyReply,
  existingRequest: NonNullable<RequestWithRelations>,
  newStatus: RequestStatusValue,
  currentUser: CurrentUser,
) {
  if (newStatus !== RequestStatus.RETURNED) {
    reply.code(400).send({
      error: 'EXPIRED request can only be set to RETURNED',
    });
    return;
  }

  const authError = canIssueRequest(currentUser);
  if (authError) {
    reply.code(authError.code).send({ error: authError.message });
    return;
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
      newValues: { status: newStatus },
    },
    req,
  );

  reply.send({ request: updatedRequest });
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
    reply.code(400).send({ error: 'request id is required' });
    return;
  }

  const statusError = validateStatusInput(body?.status);
  if (statusError) {
    reply.code(statusError.code).send({ error: statusError.message });
    return;
  }

  const statusRaw = typeof body.status === 'string' ? body.status.trim() : '';
  if (!statusRaw) {
    reply.code(400).send({ error: 'status is required' });
    return;
  }
  const newStatus = statusRaw as RequestStatusValue;

  const currentUser = getCurrentUser(req);
  const currentUserId = currentUser?.sub;
  if (!currentUserId) {
    reply.code(401).send({ error: 'unauthorized' });
    return;
  }

  try {
    await expireOverdueRequests({ requestId: id });

    const existingRequest = await fetchRequestWithItems(id);

    if (!existingRequest) {
      reply.code(404).send({ error: 'request not found' });
      return;
    }

    switch (existingRequest.status) {
      case RequestStatus.PENDING:
        await handlePendingStatusUpdate(req, reply, existingRequest, newStatus, currentUser);
        break;

      case RequestStatus.APPROVED:
        await handleApprovedStatusUpdate(req, reply, existingRequest, newStatus, currentUser);
        break;

      case RequestStatus.ISSUED: {
        const lastRenewReason = body.lastRenewReason?.trim();
        await handleIssuedStatusUpdate(
          req,
          reply,
          existingRequest,
          newStatus,
          currentUser,
          lastRenewReason,
        );
        break;
      }

      case RequestStatus.PARTIALLY_ISSUED: {
        if (newStatus === RequestStatus.ISSUED) {
          await handleApprovedStatusUpdate(req, reply, existingRequest, newStatus, currentUser);
        } else {
          const lastRenewReason = body.lastRenewReason?.trim();
          await handleIssuedStatusUpdate(
            req,
            reply,
            existingRequest,
            newStatus,
            currentUser,
            lastRenewReason,
          );
        }
        break;
      }

      case RequestStatus.REQUESTED_RENEW:
        await handleRequestedRenewalStatusUpdate(
          req,
          reply,
          existingRequest,
          newStatus,
          currentUser,
        );
        break;

      case RequestStatus.RENEWED:
      case RequestStatus.PARTIALLY_RETURNED: {
        const renewedLastRenewReason = body.lastRenewReason?.trim();
        await handleIssuedStatusUpdate(
          req,
          reply,
          existingRequest,
          newStatus,
          currentUser,
          renewedLastRenewReason,
        );
        break;
      }

      case RequestStatus.EXPIRED:
        await handleExpiredStatusUpdate(req, reply, existingRequest, newStatus, currentUser);
        break;

      default:
        reply.code(400).send({
          error:
            'request status can only be updated when status is PENDING, APPROVED, ISSUED, PARTIALLY_ISSUED, REQUESTED_RENEW, RENEWED, PARTIALLY_RETURNED, or EXPIRED',
        });
    }
  } catch (err) {
    app.log.error(err);
    reply.code(500).send({ error: 'failed to update request' });
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
    reply.code(400).send({ error: 'request id is required' });
    return;
  }

  const currentUser = getCurrentUser(req);
  const currentUserId = currentUser?.sub;
  if (!currentUserId) {
    reply.code(401).send({ error: 'unauthorized' });
    return;
  }

  try {
    await expireOverdueRequests({ requestId: id });

    const existingRequest = await db.query.request.findFirst({
      where: (r, { eq }) => eq(r.id, id),
    });

    if (!existingRequest) {
      reply.code(404).send({ error: 'request not found' });
      return;
    }

    const authError = canDeleteRequest(currentUser, existingRequest);
    if (authError) {
      reply.code(authError.code).send({ error: authError.message });
      return;
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

    notifyRequestsUpdated();

    reply.code(204).send();
  } catch (err) {
    app.log.error(err);
    reply.code(500).send({ error: 'failed to delete request' });
  }
}

const requestsRoutes: FastifyPluginCallback = (app, _opts, done) => {
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

  app.get('/requests/events', { preHandler: requireAuth }, (req, reply) => {
    const p = pushable<EventMessage>({ objectMode: true });
    const onUpdate = () => {
      p.push({ data: 'updated' });
    };

    events.on(EventType.REQUESTS_UPDATED, onUpdate);

    req.raw.on('close', () => {
      events.off(EventType.REQUESTS_UPDATED, onUpdate);
      p.end();
    });

    reply.sse(p);
  });

  done();
};

export default requestsRoutes;
