import './setup.js';

import { describe, test, before, after, beforeEach, afterEach } from 'node:test';
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
let facultyId: string;
let createdOtherUserIds: string[] = [];
let createdComponentIds: string[] = [];
const requestStatus = {
  APPROVED: 'APPROVED',
} as const;

before(async () => {
  app = await buildApp();

  await (prisma as any).requestItem.deleteMany({});
  await (prisma as any).request.deleteMany({});

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

  const facultyUser = await prisma.user.create({
    data: {
      email: `faculty_${suffix}@example.com`,
      passwordHash,
      name: 'Faculty User',
      role: UserRole.FACULTY,
    },
  });
  facultyId = facultyUser.id;
});

after(async () => {
  await (prisma as any).requestItem.deleteMany({});
  await (prisma as any).request.deleteMany({});
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
    createdComponentIds = [];
  });

  afterEach(async () => {
    await (prisma as any).requestItem.deleteMany({});
    await (prisma as any).request.deleteMany({});
    if (createdComponentIds.length > 0) {
      await prisma.component.deleteMany({ where: { id: { in: createdComponentIds } } });
    }
  });

  describe('GET /faculty - List faculty', () => {
    test('returns 401 without token', async () => {
      const response = await app.inject({
        method: 'GET',
        url: '/faculty',
      });
      assert.equal(response.statusCode, 401);
    });

    test('returns list of faculty users', async () => {
      const response = await app.inject({
        method: 'GET',
        url: '/faculty',
        headers: { authorization: `Bearer ${studentToken}` },
      });

      assert.equal(response.statusCode, 200);
      const body = response.json();
      assert.ok(Array.isArray(body.faculty));
      assert.ok(body.faculty.some((u: { id: string; role: string }) => u.id === facultyId && u.role === 'FACULTY'));
    });
  });

  describe('POST /requests - Create request', () => {
    test('returns 401 without token', async () => {
      const response = await app.inject({
        method: 'POST',
        url: '/requests',
        payload: {
          items: [{ componentId: 'component-1', quantity: 1 }],
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
      const component = await prisma.component.create({
        data: { name: 'Resistor', quantity: 10 },
      });
      createdComponentIds.push(component.id);

      const response = await app.inject({
        method: 'POST',
        url: '/requests',
        headers: {
          authorization: `Bearer ${studentToken}`,
        },
        payload: {
          items: [{ componentId: component.id, quantity: 0 }],
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
          items: [{ componentId: 'missing-component', quantity: 2 }],
        },
      });

      assert.equal(response.statusCode, 400);
    });

    test('creates a request with multiple items', async () => {
      const item1 = await prisma.component.create({
        data: { name: 'Arduino', quantity: 5 },
      });
      const item2 = await prisma.component.create({
        data: { name: 'Breadboard', quantity: 20 },
      });
      createdComponentIds.push(item1.id, item2.id);

      const response = await app.inject({
        method: 'POST',
        url: '/requests',
        headers: {
          authorization: `Bearer ${studentToken}`,
        },
        payload: {
          items: [
            { componentId: item1.id, quantity: 1 },
            { componentId: item2.id, quantity: 2 },
          ],
        },
      });

      assert.equal(response.statusCode, 201);
      const body = response.json();
      assert.equal(body.request.userId, studentId);
      assert.equal(body.request.status, 'PENDING');
      assert.equal(body.request.items.length, 2);
    });

    test('creates a request with targetFacultyId', async () => {
      const item = await prisma.component.create({
        data: { name: 'Requested to Faculty', quantity: 5 },
      });
      createdComponentIds.push(item.id);

      const response = await app.inject({
        method: 'POST',
        url: '/requests',
        headers: { authorization: `Bearer ${studentToken}` },
        payload: {
          items: [{ componentId: item.id, quantity: 1 }],
          targetFacultyId: facultyId,
        },
      });

      assert.equal(response.statusCode, 201);
      const body = response.json();
      assert.equal(body.request.targetFacultyId, facultyId);
      assert.ok(body.request.targetFaculty);
      assert.equal(body.request.targetFaculty.id, facultyId);
      assert.equal(body.request.targetFaculty.role, 'FACULTY');
    });

    test('returns 400 for invalid targetFacultyId', async () => {
      const item = await prisma.component.create({
        data: { name: 'Invalid Faculty', quantity: 5 },
      });
      createdComponentIds.push(item.id);

      const response = await app.inject({
        method: 'POST',
        url: '/requests',
        headers: { authorization: `Bearer ${studentToken}` },
        payload: {
          items: [{ componentId: item.id, quantity: 1 }],
          targetFacultyId: studentId,
        },
      });

      assert.equal(response.statusCode, 400);
    });
  });

  describe('DELETE /requests/:id - Retract request', () => {
    test('student can delete own pending request', async () => {
      const component = await prisma.component.create({
        data: { name: 'To Delete', quantity: 5 },
      });
      createdComponentIds.push(component.id);

      const createResponse = await app.inject({
        method: 'POST',
        url: '/requests',
        headers: {
          authorization: `Bearer ${studentToken}`,
        },
        payload: {
          items: [{ componentId: component.id, quantity: 1 }],
        },
      });

      assert.equal(createResponse.statusCode, 201);
      const createdId = createResponse.json().request.id;

      const deleteResponse = await app.inject({
        method: 'DELETE',
        url: `/requests/${createdId}`,
        headers: {
          authorization: `Bearer ${studentToken}`,
        },
      });

      assert.equal(deleteResponse.statusCode, 204);
      const exists = await (prisma as any).request.findUnique({ where: { id: createdId } });
      assert.equal(exists, null);
    });

    test('student cannot delete someone else request', async () => {
      const component = await prisma.component.create({
        data: { name: 'Other User Component', quantity: 5 },
      });
      createdComponentIds.push(component.id);

      const otherUser = await prisma.user.create({
        data: {
          email: `other_${Date.now()}@example.com`,
          passwordHash: 'hash',
          name: 'Other User',
          role: UserRole.STUDENT,
        },
      });
      createdOtherUserIds.push(otherUser.id);

      const request = await (prisma as any).request.create({
        data: {
          userId: otherUser.id,
          items: {
            create: [{ componentId: component.id, quantity: 1 }],
          },
        },
      });

      const deleteResponse = await app.inject({
        method: 'DELETE',
        url: `/requests/${request.id}`,
        headers: {
          authorization: `Bearer ${studentToken}`,
        },
      });

      assert.equal(deleteResponse.statusCode, 403);
    });

    test('cannot delete non-pending request', async () => {
      const component = await prisma.component.create({
        data: { name: 'Approved Component', quantity: 5 },
      });
      createdComponentIds.push(component.id);

      const request = await (prisma as any).request.create({
        data: {
          userId: studentId,
          status: requestStatus.APPROVED,
          items: {
            create: [{ componentId: component.id, quantity: 1 }],
          },
        },
      });

      const deleteResponse = await app.inject({
        method: 'DELETE',
        url: `/requests/${request.id}`,
        headers: {
          authorization: `Bearer ${studentToken}`,
        },
      });

      assert.equal(deleteResponse.statusCode, 400);
      const stillExists = await (prisma as any).request.findUnique({ where: { id: request.id } });
      assert.ok(stillExists);
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
      const item = await prisma.component.create({
        data: { name: 'Sensor', quantity: 10 },
      });
      createdComponentIds.push(item.id);

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
            create: [{ componentId: item.id, quantity: 1 }],
          },
        },
      });

      await (prisma as any).request.create({
        data: {
          userId: studentId,
          items: {
            create: [{ componentId: item.id, quantity: 1 }],
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
      const item = await prisma.component.create({
        data: { name: 'Display', quantity: 10 },
      });
      createdComponentIds.push(item.id);

      const request = await (prisma as any).request.create({
        data: {
          userId: studentId,
          status: requestStatus.APPROVED,
          items: {
            create: [{ componentId: item.id, quantity: 1 }],
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
