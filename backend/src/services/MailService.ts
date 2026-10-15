import nodemailer from 'nodemailer';
import type { Transporter } from 'nodemailer';

let transporter: Transporter | null = null;

export function getMailTransporter(): Transporter | null {
  if (transporter) return transporter;

  const host = process.env.SMTP_HOST;
  const port = Number.parseInt(process.env.SMTP_PORT ?? '', 10);
  const user = process.env.SMTP_USER;
  const pass = process.env.SMTP_PASS;
  const from = process.env.SMTP_FROM;

  if (!host || !Number.isFinite(port) || !user || !pass || !from) {
    return null;
  }

  transporter = nodemailer.createTransport({
    host,
    port,
    secure: port === 465,
    auth: { user, pass },
  });

  return transporter;
}

export async function sendMail(to: string, subject: string, text: string): Promise<boolean> {
  const tx = getMailTransporter();
  if (!tx) {
    return false;
  }

  const from = process.env.SMTP_FROM!;
  try {
    await tx.sendMail({ from, to, subject, text });
    return true;
  } catch (err) {
    console.error('Failed to send email', { to, subject, err });
    return false;
  }
}
