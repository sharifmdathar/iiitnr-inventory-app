import { and, eq, inArray, isNotNull, lte } from 'drizzle-orm';
import { db } from '../drizzle/db.js';
import { request } from '../drizzle/schema.js';
import { RequestStatus } from '../utils/enums.js';
import type { RequestStatusValue } from '../utils/enums.js';

export const REQUEST_RETURN_LIMIT_MS = 30 * 24 * 60 * 60 * 1000; // 30 days
export const REQUEST_EXPIRY_SWEEP_INTERVAL_MS = 60 * 60 * 1000; // 1 hour

const expirableStatuses: RequestStatusValue[] = [
  RequestStatus.FULFILLED,
  RequestStatus.REQUESTED_RENEW,
  RequestStatus.RENEWED,
];

interface ExpireOverdueRequestsOptions {
  requestId?: string;
  now?: Date;
}

export async function expireOverdueRequests(
  options: ExpireOverdueRequestsOptions = {},
): Promise<number> {
  const nowIso = (options.now ?? new Date()).toISOString();
  const conditions = [
    inArray(request.status, expirableStatuses),
    isNotNull(request.returnDueAt),
    lte(request.returnDueAt, nowIso),
  ];

  if (options.requestId) {
    conditions.push(eq(request.id, options.requestId));
  }

  const expiredRows = await db
    .update(request)
    .set({ status: RequestStatus.EXPIRED, updatedAt: nowIso })
    .where(and(...conditions))
    .returning({ id: request.id });

  return expiredRows.length;
}

export function startRequestExpirySweep(
  onError: (err: unknown) => void,
  intervalMs = REQUEST_EXPIRY_SWEEP_INTERVAL_MS,
) {
  const runSweep = () => {
    expireOverdueRequests().catch(onError);
  };

  runSweep();
  const interval = setInterval(runSweep, intervalMs);
  interval.unref?.();

  return () => clearInterval(interval);
}
