import { and, eq, inArray, isNotNull, isNull } from 'drizzle-orm';
import { db } from '../drizzle/db.js';
import { request, requestEmail, user } from '../drizzle/schema.js';
import { RequestStatus } from '../utils/enums.js';
import { istDaysBetween } from './date-utils.js';
import { sendTrackedEmail } from './email-service.js';
import { isMailEnabled } from './MailService.js';
import {
  buildEmailContext,
  lateFeeEmail,
  LATE_FEE_CAP_DAYS,
  LATE_FEE_LADDER,
  LATE_FEE_PER_DAY,
} from './email-templates.js';
import { fetchAndShapeRequest } from './RequestService.js';

export const LATE_FEE_INTERVAL_MS = 60 * 60 * 1000; // 1 hour

export interface LateFeeCandidate {
  requestId: string;
  recipientId: string;
  recipientEmail: string;
  daysOverdue: number;
  feeAccrued: number;
  variant: string;
}

export function computeLateFee(daysOverdue: number): number {
  if (daysOverdue <= 0) return 0;
  return Math.min(daysOverdue, LATE_FEE_CAP_DAYS) * LATE_FEE_PER_DAY;
}

/**
 * `returnedAt` is only set once everything is back, so a partially returned
 * request that later expired is still charged for what is outstanding.
 */
export async function findLateFeeCandidates(now: Date = new Date()): Promise<LateFeeCandidate[]> {
  const rows = await db
    .select({
      requestId: request.id,
      returnDueAt: request.returnDueAt,
      recipientId: user.id,
      recipientEmail: user.email,
    })
    .from(request)
    .innerJoin(user, eq(request.userId, user.id))
    .where(
      and(
        eq(request.status, RequestStatus.EXPIRED),
        isNull(request.returnedAt),
        isNotNull(request.returnDueAt),
      ),
    );

  if (!rows.length) return [];

  const sentRows = await db
    .select({ requestId: requestEmail.requestId, variant: requestEmail.variant })
    .from(requestEmail)
    .where(
      and(
        eq(requestEmail.kind, 'LATE_FEE'),
        inArray(
          requestEmail.requestId,
          rows.map((row) => row.requestId),
        ),
      ),
    );

  const alreadySent = new Set(sentRows.map((row) => `${row.requestId}:${row.variant}`));

  const candidates: LateFeeCandidate[] = [];
  for (const row of rows) {
    if (!row.returnDueAt) continue;

    const daysOverdue = istDaysBetween(new Date(row.returnDueAt), now);
    if (daysOverdue < 1) continue;

    const due = LATE_FEE_LADDER.filter((rung) => daysOverdue >= rung.day)
      .filter((rung) => !alreadySent.has(`${row.requestId}:${rung.variant}`))
      .pop();
    if (!due) continue;

    candidates.push({
      requestId: row.requestId,
      recipientId: row.recipientId,
      recipientEmail: row.recipientEmail,
      daysOverdue,
      feeAccrued: computeLateFee(daysOverdue),
      variant: due.variant,
    });
  }

  return candidates;
}

/** Keeps `Request.lateFeeAccrued` in step with the overdue period. */
export async function syncAccruedFees(now: Date = new Date()): Promise<number> {
  const rows = await db
    .select({
      requestId: request.id,
      returnDueAt: request.returnDueAt,
      accrued: request.lateFeeAccrued,
    })
    .from(request)
    .where(
      and(
        eq(request.status, RequestStatus.EXPIRED),
        isNull(request.returnedAt),
        isNotNull(request.returnDueAt),
      ),
    );

  let updated = 0;
  for (const row of rows) {
    if (!row.returnDueAt) continue;
    const fee = computeLateFee(istDaysBetween(new Date(row.returnDueAt), now));
    if (fee === row.accrued) continue;

    await db
      .update(request)
      .set({ lateFeeAccrued: fee, updatedAt: now.toISOString() })
      .where(eq(request.id, row.requestId));
    updated++;
  }

  return updated;
}

export async function runLateFeeSweep(now: Date = new Date()): Promise<{
  candidates: number;
  sent: number;
  failed: number;
  feesUpdated: number;
  skipped: boolean;
}> {
  // The charge is a record and keeps accruing either way — only the mails
  // are optional.
  const feesUpdated = await syncAccruedFees(now);

  if (!isMailEnabled()) {
    return { candidates: 0, sent: 0, failed: 0, feesUpdated, skipped: true };
  }

  const candidates = await findLateFeeCandidates(now);

  let sent = 0;
  let failed = 0;

  for (const candidate of candidates) {
    const shaped = await fetchAndShapeRequest(candidate.requestId);
    if (!shaped) {
      failed++;
      continue;
    }

    const ctx = buildEmailContext(shaped);
    const { subject, text } = lateFeeEmail(
      ctx,
      candidate.daysOverdue,
      candidate.feeAccrued,
      candidate.variant,
    );

    const ok = await sendTrackedEmail({
      requestId: candidate.requestId,
      recipientId: candidate.recipientId,
      kind: 'LATE_FEE',
      variant: candidate.variant,
      to: candidate.recipientEmail,
      subject,
      text,
      metadata: {
        daysOverdue: candidate.daysOverdue,
        feeAccrued: candidate.feeAccrued,
      },
    });

    if (ok) sent++;
    else failed++;
  }

  return { candidates: candidates.length, sent, failed, feesUpdated, skipped: false };
}

export function startLateFeeSweep(
  onError: (err: unknown) => void,
  intervalMs = LATE_FEE_INTERVAL_MS,
) {
  const runSweep = () => {
    runLateFeeSweep()
      .then((result) => {
        if (result.sent > 0 || result.feesUpdated > 0) {
          console.log('Late fee sweep', result);
        }
      })
      .catch(onError);
  };

  runSweep();
  const interval = setInterval(runSweep, intervalMs);
  interval.unref?.();

  return () => clearInterval(interval);
}
