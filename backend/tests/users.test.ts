import './test-setup.js';

import { describe, test, beforeAll, afterAll } from 'bun:test';
import assert from 'node:assert/strict';
import { inArray } from 'drizzle-orm';
import { buildApp } from '../src/app.js';
import { db } from '../src/drizzle/db.js';
import { auditLog } from '../src/drizzle/schema.js';
import { createUser, deleteAllData, deleteUsers, UserRole } from './helpers.js';
import { AuditActionType } from '../src/utils/enums.js';

let app: Awaited<ReturnType<typeof buildApp>>;
let adminToken: string;
let taToken: string;
let studentToken: string;
let adminUserId: string;
let taUserId: string;
let studentUserId: string;
let otherUserIds: string[] = [];

beforeAll(async () => {
  app = await buildApp();

  const { hash } = await import('bcryptjs');
  const passwordHash = await hash('password123', 12);

  const adminUser = await createUser({
    email: `admin_users_${crypto.randomUUID()}@example.com`,
    passwordHash,
    name: 'Admin User',
    role: UserRole.ADMIN,
  });
  adminUserId = adminUser.id;
  adminToken = app.jwt.sign({ sub: adminUser.id, role: adminUser.role }, { expiresIn: '1h' });

  const taUser = await createUser({
    email: `ta_users_${crypto.randomUUID()}@example.com`,
    passwordHash,
    name: 'TA User',
    role: UserRole.TA,
  });
  taUserId = taUser.id;
  taToken = app.jwt.sign({ sub: taUser.id, role: taUser.role }, { expiresIn: '1h' });

  const studentUser = await createUser({
    email: `student_users_${crypto.randomUUID()}@example.com`,
    passwordHash,
    name: 'Student User',
    role: UserRole.STUDENT,
  });
  studentUserId = studentUser.id;
  studentToken = app.jwt.sign({ sub: studentUser.id, role: studentUser.role }, { expiresIn: '1h' });
});

afterAll(async () => {
  await db
    .delete(auditLog)
    .where(
      inArray(
        auditLog.userId,
        [adminUserId, taUserId, studentUserId, ...otherUserIds].filter(Boolean),
      ),
    );
  await deleteAllData();
  const allUserIds = [adminUserId, taUserId, studentUserId, ...otherUserIds].filter(Boolean);
  await deleteUsers(allUserIds);
  await app.close();
});

describe('User Management API', () => {
  describe('GET /admin/users', () => {
    test('returns all users with pagination', async () => {
      const response = await app.inject({
        method: 'GET',
        url: '/admin/users',
        headers: { authorization: `Bearer ${adminToken}` },
      });

      assert.equal(response.statusCode, 200);
      const body = response.json();
      assert.ok(Array.isArray(body.users));
      assert.ok(body.users.length >= 3);
      assert.ok(body.pagination);
      assert.equal(body.pagination.limit, 50);
      assert.equal(body.pagination.offset, 0);
      assert.equal(body.pagination.total, body.users.length);
    });

    test('filters users by search query (name)', async () => {
      const searchEmail = `admin_users_${crypto.randomUUID()}@example.com`;
      const { hash } = await import('bcryptjs');
      const passwordHash = await hash('password123', 12);
      const targetUser = await createUser({
        email: searchEmail,
        passwordHash,
        name: 'UniqueSearchName',
        role: UserRole.STUDENT,
      });
      otherUserIds.push(targetUser.id);

      const response = await app.inject({
        method: 'GET',
        url: '/admin/users?search=UniqueSearchName',
        headers: { authorization: `Bearer ${adminToken}` },
      });

      assert.equal(response.statusCode, 200);
      const body = response.json();
      assert.equal(body.users.length, 1);
      assert.equal(body.users[0].email, searchEmail);
    });

    test('filters users by search query (email)', async () => {
      const searchEmail = `exact_${crypto.randomUUID()}@example.com`;
      const { hash } = await import('bcryptjs');
      const passwordHash = await hash('password123', 12);
      const targetUser = await createUser({
        email: searchEmail,
        passwordHash,
        name: 'Email Search User',
        role: UserRole.STUDENT,
      });
      otherUserIds.push(targetUser.id);

      const response = await app.inject({
        method: 'GET',
        url: `/admin/users?search=${searchEmail}`,
        headers: { authorization: `Bearer ${adminToken}` },
      });

      assert.equal(response.statusCode, 200);
      const body = response.json();
      assert.equal(body.users.length, 1);
      assert.equal(body.users[0].email, searchEmail);
    });

    test('respects pagination limit and offset', async () => {
      const response = await app.inject({
        method: 'GET',
        url: '/admin/users?limit=2&offset=0',
        headers: { authorization: `Bearer ${adminToken}` },
      });

      assert.equal(response.statusCode, 200);
      const body = response.json();
      assert.ok(body.users.length <= 2);
      assert.equal(body.pagination.limit, 2);
      assert.equal(body.pagination.offset, 0);
    });

    test('allows admin but rejects TA', async () => {
      const response = await app.inject({
        method: 'GET',
        url: '/admin/users',
        headers: { authorization: `Bearer ${taToken}` },
      });

      assert.equal(response.statusCode, 403);
    });

    test('rejects student', async () => {
      const response = await app.inject({
        method: 'GET',
        url: '/admin/users',
        headers: { authorization: `Bearer ${studentToken}` },
      });

      assert.equal(response.statusCode, 403);
    });

    test('rejects unauthenticated request', async () => {
      const response = await app.inject({
        method: 'GET',
        url: '/admin/users',
      });

      assert.equal(response.statusCode, 401);
    });

    test('requires missing search to return all users', async () => {
      const response = await app.inject({
        method: 'GET',
        url: '/admin/users?search=',
        headers: { authorization: `Bearer ${adminToken}` },
      });

      assert.equal(response.statusCode, 200);
      const body = response.json();
      assert.ok(body.users.length >= 3);
    });

    test('caps limit at 200', async () => {
      const response = await app.inject({
        method: 'GET',
        url: '/admin/users?limit=999',
        headers: { authorization: `Bearer ${adminToken}` },
      });

      assert.equal(response.statusCode, 200);
      const body = response.json();
      assert.equal(body.pagination.limit, 200);
    });
  });

  describe('PATCH /admin/users/:id', () => {
    test('updates user name, role, batch, and branch', async () => {
      const targetUser = await createUser({
        email: `update_${crypto.randomUUID()}@example.com`,
        name: 'Original Name',
        role: UserRole.PENDING,
      });
      otherUserIds.push(targetUser.id);

      const response = await app.inject({
        method: 'PATCH',
        url: `/admin/users/${targetUser.id}`,
        headers: { authorization: `Bearer ${adminToken}` },
        payload: {
          name: 'Updated Name',
          role: 'STUDENT',
          batch: '2024-2028',
          branch: 'CSE',
        },
      });

      assert.equal(response.statusCode, 200);
      const body = response.json();
      assert.equal(body.user.name, 'Updated Name');
      assert.equal(body.user.role, 'STUDENT');
      assert.equal(body.user.batch, '2024-2028');
      assert.equal(body.user.branch, 'CSE');
    });

    test('updates only provided fields', async () => {
      const targetUser = await createUser({
        email: `partial_${crypto.randomUUID()}@example.com`,
        name: 'Partial User',
        role: UserRole.PENDING,
      });
      otherUserIds.push(targetUser.id);

      const response = await app.inject({
        method: 'PATCH',
        url: `/admin/users/${targetUser.id}`,
        headers: { authorization: `Bearer ${adminToken}` },
        payload: {
          role: 'FACULTY',
        },
      });

      assert.equal(response.statusCode, 200);
      const body = response.json();
      assert.equal(body.user.role, 'FACULTY');
      assert.equal(body.user.name, 'Partial User');
      assert.equal(body.user.batch, null);
      assert.equal(body.user.branch, null);
    });

    test('rejects invalid role', async () => {
      const targetUser = await createUser({
        email: `invalidrole_${crypto.randomUUID()}@example.com`,
        role: UserRole.PENDING,
      });
      otherUserIds.push(targetUser.id);

      const response = await app.inject({
        method: 'PATCH',
        url: `/admin/users/${targetUser.id}`,
        headers: { authorization: `Bearer ${adminToken}` },
        payload: {
          role: 'SUPER_ADMIN',
        },
      });

      assert.equal(response.statusCode, 400);
      assert.ok(response.json().error.includes('invalid role'));
    });

    test('returns 404 for non-existent user', async () => {
      const response = await app.inject({
        method: 'PATCH',
        url: '/admin/users/nonexistent-id-12345',
        headers: { authorization: `Bearer ${adminToken}` },
        payload: {
          name: 'Ghost User',
        },
      });

      assert.equal(response.statusCode, 404);
    });

    test('rejects TA', async () => {
      const targetUser = await createUser({
        email: `tareject_${crypto.randomUUID()}@example.com`,
        role: UserRole.PENDING,
      });
      otherUserIds.push(targetUser.id);

      const response = await app.inject({
        method: 'PATCH',
        url: `/admin/users/${targetUser.id}`,
        headers: { authorization: `Bearer ${taToken}` },
        payload: {
          role: 'STUDENT',
        },
      });

      assert.equal(response.statusCode, 403);
    });

    test('rejects student', async () => {
      const targetUser = await createUser({
        email: `studentreject_${crypto.randomUUID()}@example.com`,
        role: UserRole.PENDING,
      });
      otherUserIds.push(targetUser.id);

      const response = await app.inject({
        method: 'PATCH',
        url: `/admin/users/${targetUser.id}`,
        headers: { authorization: `Bearer ${studentToken}` },
        payload: {
          role: 'STUDENT',
        },
      });

      assert.equal(response.statusCode, 403);
    });

    test('rejects unauthenticated request', async () => {
      const targetUser = await createUser({
        email: `unauth_${crypto.randomUUID()}@example.com`,
        role: UserRole.PENDING,
      });
      otherUserIds.push(targetUser.id);

      const response = await app.inject({
        method: 'PATCH',
        url: `/admin/users/${targetUser.id}`,
        payload: {
          role: 'STUDENT',
        },
      });

      assert.equal(response.statusCode, 401);
    });

    test('logs audit entry on update', async () => {
      const targetUser = await createUser({
        email: `audit_${crypto.randomUUID()}@example.com`,
        name: 'Audit User',
        role: UserRole.PENDING,
      });
      otherUserIds.push(targetUser.id);

      const response = await app.inject({
        method: 'PATCH',
        url: `/admin/users/${targetUser.id}`,
        headers: { authorization: `Bearer ${adminToken}` },
        payload: {
          role: 'STUDENT',
          batch: '2025-2029',
        },
      });

      assert.equal(response.statusCode, 200);

      const logs = await db
        .select()
        .from(auditLog)
        .where(inArray(auditLog.entityId, [targetUser.id]));

      const updateLog = logs.find(
        (l) => l.action === AuditActionType.UPDATE && l.entityType === 'User',
      );
      assert.ok(updateLog, 'UPDATE audit log should exist');
      if (!updateLog) throw new Error('Log not found');
      assert.equal(updateLog.entityType, 'User');
      assert.equal(updateLog.entityId, targetUser.id);
      assert.ok(updateLog.oldValues);
      assert.ok(updateLog.newValues);

      const oldValues = JSON.parse(updateLog.oldValues as string);
      const newValues = JSON.parse(updateLog.newValues as string);
      assert.equal(oldValues.role, 'PENDING');
      assert.equal(newValues.role, 'STUDENT');
      assert.equal(newValues.batch, '2025-2029');
    });

    test('clears fields when empty string provided', async () => {
      const targetUser = await createUser({
        email: `clear_${crypto.randomUUID()}@example.com`,
        name: 'Clear Test',
        role: UserRole.STUDENT,
        batch: '2024-2028',
        branch: 'CSE',
      });
      otherUserIds.push(targetUser.id);

      const response = await app.inject({
        method: 'PATCH',
        url: `/admin/users/${targetUser.id}`,
        headers: { authorization: `Bearer ${adminToken}` },
        payload: {
          batch: '',
          branch: '',
        },
      });

      assert.equal(response.statusCode, 200);
      const body = response.json();
      assert.equal(body.user.batch, null);
      assert.equal(body.user.branch, null);
    });
  });
});
