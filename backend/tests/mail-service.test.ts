import { afterEach, beforeEach, describe, expect, test } from 'bun:test';
import { isMailEnabled, resolveMailConfig, sendMail } from '../src/services/MailService.js';

const ENV_KEYS = [
  'MAIL_ENABLED',
  'SMTP_HOST',
  'SMTP_PORT',
  'SMTP_USER',
  'SMTP_PASS',
  'SMTP_FROM',
] as const;

let saved: Record<string, string | undefined> = {};

beforeEach(() => {
  saved = {};
  for (const key of ENV_KEYS) {
    saved[key] = process.env[key];
    delete process.env[key];
  }
});

afterEach(() => {
  for (const key of ENV_KEYS) {
    if (saved[key] === undefined) delete process.env[key];
    else process.env[key] = saved[key];
  }
});

describe('resolveMailConfig', () => {
  test('resolves a complete configuration', () => {
    process.env.SMTP_HOST = 'smtp.example.com';
    process.env.SMTP_PORT = '587';
    process.env.SMTP_USER = 'inventory@example.com';
    process.env.SMTP_PASS = 'secret';
    process.env.SMTP_FROM = 'Inventory Mgmt System <inventory@example.com>';

    const config = resolveMailConfig();
    expect(config).not.toBeNull();
    expect(config?.host).toBe('smtp.example.com');
    expect(config?.port).toBe(587);
    expect(config?.secure).toBe(false);
    expect(config?.requireTLS).toBe(true);
    expect(config?.user).toBe('inventory@example.com');
    expect(config?.from).toBe('Inventory Mgmt System <inventory@example.com>');
  });

  test('returns null when the password is missing', () => {
    process.env.SMTP_HOST = 'smtp.example.com';
    process.env.SMTP_PORT = '587';
    process.env.SMTP_USER = 'inventory@example.com';
    process.env.SMTP_FROM = 'Inventory Mgmt System <inventory@example.com>';

    expect(resolveMailConfig()).toBeNull();
  });

  test('returns null when host, port, user or from are missing', () => {
    process.env.SMTP_HOST = 'smtp.example.com';
    process.env.SMTP_PORT = '587';
    process.env.SMTP_USER = 'inventory@example.com';
    process.env.SMTP_PASS = 'secret';
    expect(resolveMailConfig()).toBeNull();

    process.env.SMTP_FROM = 'Inventory Mgmt System <inventory@example.com>';
    delete process.env.SMTP_PORT;
    expect(resolveMailConfig()).toBeNull();
  });

  test('uses implicit TLS on port 465', () => {
    process.env.SMTP_HOST = 'smtp.example.com';
    process.env.SMTP_PORT = '465';
    process.env.SMTP_USER = 'inventory@example.com';
    process.env.SMTP_PASS = 'secret';
    process.env.SMTP_FROM = 'Inventory Mgmt System <inventory@example.com>';

    const config = resolveMailConfig();
    expect(config?.secure).toBe(true);
    expect(config?.requireTLS).toBe(false);
  });
});

describe('isMailEnabled', () => {
  test('is off unless it is explicitly turned on', () => {
    expect(isMailEnabled()).toBe(false);

    process.env.MAIL_ENABLED = 'false';
    expect(isMailEnabled()).toBe(false);

    process.env.MAIL_ENABLED = 'no';
    expect(isMailEnabled()).toBe(false);

    process.env.MAIL_ENABLED = '';
    expect(isMailEnabled()).toBe(false);
  });

  test('is on for the usual ways of saying yes', () => {
    process.env.MAIL_ENABLED = 'true';
    expect(isMailEnabled()).toBe(true);

    process.env.MAIL_ENABLED = 'TRUE';
    expect(isMailEnabled()).toBe(true);

    process.env.MAIL_ENABLED = ' True ';
    expect(isMailEnabled()).toBe(true);
  });
});

describe('sendMail', () => {
  test('does not send anything while mail is disabled', async () => {
    delete process.env.MAIL_ENABLED;
    process.env.SMTP_HOST = 'smtp.example.com';
    process.env.SMTP_PORT = '587';
    process.env.SMTP_USER = 'inventory@example.com';
    process.env.SMTP_PASS = 'secret';
    process.env.SMTP_FROM = 'Inventory Mgmt System <inventory@example.com>';

    const sent = await sendMail('someone@example.com', 'Should never be sent', 'body');

    expect(sent).toBe(false);
  });

  test('rejects an address that cannot be delivered to', async () => {
    process.env.MAIL_ENABLED = 'true';

    const sent = await sendMail('not-an-address', 'Subject', 'body');

    expect(sent).toBe(false);
  });
});
