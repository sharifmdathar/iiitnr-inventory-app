import { test, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { buildApp } from '../src/app.js';

process.env.JWT_SECRET = process.env.JWT_SECRET ?? 'test-secret';

let app: Awaited<ReturnType<typeof buildApp>>;

before(async () => {
  app = await buildApp();
});

after(async () => {
  await app.close();
});

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
