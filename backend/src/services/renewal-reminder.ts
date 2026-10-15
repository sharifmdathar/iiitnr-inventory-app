import { and, eq, gte, inArray, isNotNull, isNull, lte } from 'drizzle-orm';
import { db } from '../drizzle/db.js';
import { request } from '../drizzle/schema.js';
import { user } from '../drizzle/schema.js';
import { RequestStatus } from '../utils/enums.js';
import type { RequestStatusValue } from '../utils/enums.js';
import { sendMail } from './MailService.js';

const RENEWAL_REMINDER_DAYS = 7;
const RENEWABLE_STATUSES: RequestStatusValue[] = [
  RequestStatus.ISSUED,
  RequestStatus.PARTIALLY_ISSUED,
  RequestStatus.PARTIALLY_RETURNED,
  RequestStatus.RENEWED,
];

export interface RenewalReminderCandidate {
  requestId: string;
  userId: string;
  userEmail: string;
  projectTitle: string;
  returnDueAt: string;
}

export async function findRenewalReminderCandidates(
  now: Date = new Date(),
): Promise<RenewalReminderCandidate[]> {
  const istNow = new Date(now.toLocaleString('en-US', { timeZone: 'Asia/Kolkata' }));
  const windowEnd = new Date(istNow);
  windowEnd.setDate(windowEnd.getDate() + RENEWAL_REMINDER_DAYS);
  windowEnd.setHours(23, 59, 59, 999);

  const rows = await db
    .select({
      requestId: request.id,
      userId: request.userId,
      userEmail: user.email,
      projectTitle: request.projectTitle,
      returnDueAt: request.returnDueAt,
    })
    .from(request)
    .innerJoin(user, eq(request.userId, user.id))
    .where(
      and(
        isNotNull(request.returnDueAt),
        inArray(request.status, RENEWABLE_STATUSES),
        isNull(request.renewalReminderSentAt),
        gte(request.returnDueAt, istNow.toISOString()),
        lte(request.returnDueAt, windowEnd.toISOString()),
      ),
    );

  return rows as RenewalReminderCandidate[];
}

function formatISTDate(iso: string): string {
  const d = new Date(iso);
  return d.toLocaleDateString('en-IN', {
    timeZone: 'Asia/Kolkata',
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
}

export async function sendRenewalReminders(
  candidates: RenewalReminderCandidate[],
): Promise<{ sent: number; failed: number }> {
  let sent = 0;
  let failed = 0;

  for (const c of candidates) {
    const dueText = formatISTDate(c.returnDueAt);
    const subject = `Reminder: Return due for "${c.projectTitle}" on ${dueText}`;
    const text = `Hi,

This is a reminder that your request "${c.projectTitle}" is due for return on ${dueText}.

Please return the items or request a renewal before the due date.

— IIITNR Inventory System`;

    const ok = await sendMail(c.userEmail, subject, text);
    if (ok) {
      sent++;
      await markReminderSent(c.requestId);
    } else {
      failed++;
    }
  }

  return { sent, failed };
}

async function markReminderSent(requestId: string): Promise<void> {
  const nowIso = new Date().toISOString();
  await db
    .update(request)
    .set({ renewalReminderSentAt: nowIso, updatedAt: nowIso })
    .where(eq(request.id, requestId));
}

export async function runRenewalReminderSweep(): Promise<{
  candidates: number;
  sent: number;
  failed: number;
}> {
  const candidates = await findRenewalReminderCandidates();
  const { sent, failed } = await sendRenewalReminders(candidates);
  return { candidates: candidates.length, sent, failed };
}

export function startRenewalReminderSweep(
  onError: (err: unknown) => void,
  intervalMs = 60 * 60 * 1000,
) {
  const runSweep = async () => {
    try {
      const result = await runRenewalReminderSweep();
      if (result.candidates > 0) {
        console.log('Renewal reminder sweep', result);
      }
    } catch (err) {
      onError(err);
    }
  };

  runSweep();
  const interval = setInterval(runSweep, intervalMs);
  interval.unref?.();

  return () => clearInterval(interval);
}
