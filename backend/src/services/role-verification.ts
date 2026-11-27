import { isMailEnabled } from './MailService.js';
import { sendDirectEmail } from './email-service.js';
import type { UserRoleValue } from '../utils/enums.js';

interface SignupUser {
  id: string;
  email: string;
  name: string | null;
}

function reviewRecipient(): string {
  return (process.env.ADMIN_EMAIL ?? '').trim();
}

export async function notifyRoleReviewRequest(
  user: SignupUser,
  requestedRole: UserRoleValue,
  grantedRole: UserRoleValue,
): Promise<void> {
  if (!isMailEnabled()) return;

  const to = reviewRecipient();
  if (!to) return;

  const outcome =
    grantedRole === requestedRole
      ? `Granted ${requestedRole} automatically (institute email pattern).`
      : 'Currently STUDENT.';
  const subject = `[Inventory] ${requestedRole} role request needs review: ${user.email}`;
  const text = [
    'A new user signed up and requested an elevated role.',
    '',
    `Name: ${user.name ?? '(no name)'}`,
    `Email: ${user.email}`,
    `Requested role: ${requestedRole}`,
    `Outcome: ${outcome}`,
    '',
    'Review and elevate them under Manage Users in the app.',
  ].join('\n');

  try {
    await sendDirectEmail(to, subject, text);
  } catch (err) {
    console.error('Failed to send role review notification', {
      userId: user.id,
      email: user.email,
      requestedRole,
      grantedRole,
      err,
    });
  }
}
