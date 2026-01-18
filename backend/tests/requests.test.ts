import './setup.js';

import { describe, test, before, after, beforeEach } from 'node:test';
import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import { buildApp } from '../src/app.js';
import { prisma } from '../src/lib/prisma.js';
import { UserRole } from '@prisma/client';

let app: Awaited<ReturnType<typeof buildApp>>;
let adminToken: string;
let studentToken: string;
let studentId: string;
let adminUserId: string;
let createdOtherUserIds: string[] = [];
const requestStatus = {
  APPROVED: 'APPROVED',
} as const;

before(async () => {
  app = await buildApp();

  await (prisma as any).requestItem.deleteMany({});
  await (prisma as any).request.deleteMany({});
  await prisma.inventoryItem.deleteMany({});

  const suffix = randomUUID();
  const adminEmail = `admin_${suffix}@example.com`;
  const studentEmail = `student_${suffix}@example.com`;

  const { hash } = await import('bcryptjs');
  const passwordHash = await hash('password123', 12);

  const adminUser = await prisma.user.create({
    data: {
      email: adminEmail,
      passwordHash,
      name: 'Admin User',
      role: UserRole.ADMIN,
    },
  });
  adminUserId = adminUser.id;
  adminToken = app.jwt.sign({ sub: adminUser.id, role: adminUser.role }, { expiresIn: '1d' });

  const studentResponse = await app.inject({
    method: 'POST',
    url: '/auth/register',
    payload: {
      email: studentEmail,
      password: 'password123',
      name: 'Student User',
      role: UserRole.STUDENT,
    },
  });
  studentToken = studentResponse.json().token;
  studentId = studentResponse.json().user.id;
});

after(async () => {
  await (prisma as any).requestItem.deleteMany({});
  await (prisma as any).request.deleteMany({});
  await prisma.inventoryItem.deleteMany({});
  const userIds = [adminUserId, studentId, ...createdOtherUserIds].filter(Boolean);
  if (userIds.length > 0) {
    await prisma.user.deleteMany({ where: { id: { in: userIds } } });
  }
  await app.close();
});

describe('Request API', () => {
  beforeEach(async () => {
    await (prisma as any).requestItem.deleteMany({});
    await (prisma as any).request.deleteMany({});
    await prisma.inventoryItem.deleteMany({});
  });

  describe('POST /requests - Create request', () => {
    test('returns 401 without token', async () => {
      const response = await app.inject({
        method: 'POST',
        url: '/requests',
        payload: {
          items: [{ itemId: 'item-1', quantity: 1 }],
        },
      });

      assert.equal(response.statusCode, 401);
    });

    test('returns 400 when items are missing', async () => {
      const response = await app.inject({
        method: 'POST',
        url: '/requests',
        headers: {
          authorization: `Bearer ${studentToken}`,
        },
        payload: {},
      });

      assert.equal(response.statusCode, 400);
    });

    test('returns 400 when quantity is invalid', async () => {
      const item = await prisma.inventoryItem.create({
        data: { name: 'Resistor', quantity: 10 },
      });

      const response = await app.inject({
        method: 'POST',
        url: '/requests',
        headers: {
          authorization: `Bearer ${studentToken}`,
        },
        payload: {
          items: [{ itemId: item.id, quantity: 0 }],
        },
      });

      assert.equal(response.statusCode, 400);
    });

    test('returns 400 when item does not exist', async () => {
      const response = await app.inject({
        method: 'POST',
        url: '/requests',
        headers: {
          authorization: `Bearer ${studentToken}`,
        },
        payload: {
          items: [{ itemId: 'missing-item', quantity: 2 }],
        },
      });

      assert.equal(response.statusCode, 400);
    });

    test('creates a request with multiple items', async () => {
      const item1 = await prisma.inventoryItem.create({
        data: { name: 'Arduino', quantity: 5 },
      });
      const item2 = await prisma.inventoryItem.create({
        data: { name: 'Breadboard', quantity: 20 },
      });

      const response = await app.inject({
        method: 'POST',
        url: '/requests',
        headers: {
          authorization: `Bearer ${studentToken}`,
        },
        payload: {
          items: [
            { itemId: item1.id, quantity: 1 },
            { itemId: item2.id, quantity: 2 },
          ],
        },
      });

      assert.equal(response.statusCode, 201);
      const body = response.json();
      assert.equal(body.request.userId, studentId);
      assert.equal(body.request.status, 'PENDING');
      assert.equal(body.request.items.length, 2);
    });
  });

  describe('GET /requests - List by user/status', () => {
    test('returns 401 without token', async () => {
      const response = await app.inject({
        method: 'GET',
        url: '/requests',
      });

      assert.equal(response.statusCode, 401);
    });

    test('student only sees own requests even with userId filter', async () => {
      const item = await prisma.inventoryItem.create({
        data: { name: 'Sensor', quantity: 10 },
      });

      const otherUser = await prisma.user.create({
        data: {
          email: `other_${Date.now()}@example.com`,
          passwordHash: 'hash',
          name: 'Other User',
          role: UserRole.STUDENT,
        },
      });
      createdOtherUserIds.push(otherUser.id);

      await (prisma as any).request.create({
        data: {
          userId: otherUser.id,
          items: {
            create: [{ itemId: item.id, quantity: 1 }],
          },
        },
      });

      await (prisma as any).request.create({
        data: {
          userId: studentId,
          items: {
            create: [{ itemId: item.id, quantity: 1 }],
          },
        },
      });

      const response = await app.inject({
        method: 'GET',
        url: `/requests?userId=${otherUser.id}`,
        headers: {
          authorization: `Bearer ${studentToken}`,
        },
      });

      assert.equal(response.statusCode, 200);
      const body = response.json();
      assert.equal(body.requests.length, 1);
      assert.equal(body.requests[0].userId, studentId);
    });

    test('admin can filter by user and status', async () => {
      const item = await prisma.inventoryItem.create({
        data: { name: 'Display', quantity: 10 },
      });

      const request = await (prisma as any).request.create({
        data: {
          userId: studentId,
          status: requestStatus.APPROVED,
          items: {
            create: [{ itemId: item.id, quantity: 1 }],
          },
        },
      });

      const response = await app.inject({
        method: 'GET',
        url: `/requests?userId=${studentId}&status=${requestStatus.APPROVED}`,
        headers: {
          authorization: `Bearer ${adminToken}`,
        },
      });

      assert.equal(response.statusCode, 200);
      const body = response.json();
      assert.ok(body.requests.some((req: { id: string }) => req.id === request.id));
    });
  });
});
