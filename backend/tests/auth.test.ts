import './setup.js';

import { describe, test, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { buildApp } from '../src/app.js';
import { prisma } from '../src/lib/prisma.js';

let app: Awaited<ReturnType<typeof buildApp>>;
const createdUserIds: string[] = [];

before(async () => {
  app = await buildApp();
});

after(async () => {
  if (createdUserIds.length > 0) {
    await prisma.user.deleteMany({ where: { id: { in: createdUserIds } } });
  }
  await app.close();
});

describe('Authentication API', () => {
  test('registers a user and logs in', async () => {
    const email = `user_${Date.now()}@example.com`;
    const password = 'password123';

    const registerResponse = await app.inject({
      method: 'POST',
      url: '/auth/register',
      payload: {
        email,
        password,
        name: 'Test User',
        role: 'STUDENT',
      },
    });

    assert.equal(registerResponse.statusCode, 201);

    const registerBody = registerResponse.json();
    assert.equal(registerBody.user.email, email);
    assert.equal(registerBody.user.role, 'STUDENT');
    assert.ok(registerBody.token);
    createdUserIds.push(registerBody.user.id);

    const loginResponse = await app.inject({
      method: 'POST',
      url: '/auth/login',
      payload: {
        email,
        password,
      },
    });

    assert.equal(loginResponse.statusCode, 200);

    const loginBody = loginResponse.json();
    assert.equal(loginBody.user.email, email);
    assert.equal(loginBody.user.role, 'STUDENT');
    assert.ok(loginBody.token);
  });

  describe('POST /auth/google - Google Sign-In', () => {
    test('returns 400 when idToken is missing', async () => {
      const response = await app.inject({
        method: 'POST',
        url: '/auth/google',
        payload: {},
      });

      assert.equal(response.statusCode, 400);
      const body = response.json();
      assert.ok(body.error.includes('idToken is required'));
    });

    test('returns 500 when GOOGLE_CLIENT_ID is not configured', async () => {
      const originalClientId = process.env.GOOGLE_CLIENT_ID;
      delete process.env.GOOGLE_CLIENT_ID;

      const response = await app.inject({
        method: 'POST',
        url: '/auth/google',
        payload: {
          idToken: 'test-token',
        },
      });

      assert.equal(response.statusCode, 500);
      const body = response.json();
      assert.ok(body.error.includes('Google OAuth not configured'));

      if (originalClientId) {
        process.env.GOOGLE_CLIENT_ID = originalClientId;
      }
    });

    test('returns 400 for invalid Google token', async () => {
      const originalClientId = process.env.GOOGLE_CLIENT_ID;
      process.env.GOOGLE_CLIENT_ID = 'test-client-id.apps.googleusercontent.com';

      const response = await app.inject({
        method: 'POST',
        url: '/auth/google',
        payload: {
          idToken: 'invalid-token',
        },
      });

      assert.ok([400, 401].includes(response.statusCode));

      if (originalClientId) {
        process.env.GOOGLE_CLIENT_ID = originalClientId;
      } else {
        delete process.env.GOOGLE_CLIENT_ID;
      }
    });

    test('validates email domain restriction', async () => {
      const originalClientId = process.env.GOOGLE_CLIENT_ID;
      const originalDomain = process.env.ALLOWED_EMAIL_DOMAIN;

      process.env.GOOGLE_CLIENT_ID = 'test-client-id.apps.googleusercontent.com';
      process.env.ALLOWED_EMAIL_DOMAIN = '@iiitnr.edu.in';

      const response = await app.inject({
        method: 'POST',
        url: '/auth/google',
        payload: {
          idToken: 'invalid-token-for-domain-test',
        },
      });

      assert.ok([400, 401].includes(response.statusCode));

      if (originalClientId) {
        process.env.GOOGLE_CLIENT_ID = originalClientId;
      } else {
        delete process.env.GOOGLE_CLIENT_ID;
      }
      if (originalDomain) {
        process.env.ALLOWED_EMAIL_DOMAIN = originalDomain;
      } else {
        delete process.env.ALLOWED_EMAIL_DOMAIN;
      }
    });
  });
});
