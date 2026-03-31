import './test-setup.js';

import { describe, test, beforeAll, afterAll } from 'bun:test';
import assert from 'node:assert/strict';
import { buildApp } from '../src/app.js';
import { db } from '../src/drizzle/db.js';
import { auditLog, component } from '../src/drizzle/schema.js';
import { eq, and } from 'drizzle-orm';
import {
  createUser,
  createComponent,
  deleteAllData,
  deleteUsers,
  ComponentCategory,
  Location,
  UserRole,
} from './helpers.js';
import { AuditActionType } from '../src/utils/enums.js';

let app: Awaited<ReturnType<typeof buildApp>>;
let adminToken: string;
let taToken: string;
let studentToken: string;
let facultyToken: string;

let adminUserId: string;
let taUserId: string;
let studentUserId: string;
let facultyUserId: string;

async function getAuditLogs(filters: { userId?: string; entityType?: string; entityId?: string }) {
  const conditions = [];
  if (filters.userId) conditions.push(eq(auditLog.userId, filters.userId));
  if (filters.entityType) conditions.push(eq(auditLog.entityType, filters.entityType));
  if (filters.entityId) conditions.push(eq(auditLog.entityId, filters.entityId));

  const whereClause = conditions.length > 0 ? and(...conditions) : undefined;

  const logs = await db.select().from(auditLog).where(whereClause);
  return logs;
}

beforeAll(async () => {
  app = await buildApp();

  const { hash } = await import('bcryptjs');
  const passwordHash = await hash('password123', 12);

  const adminUser = await createUser({
    email: `admin_audit_${crypto.randomUUID()}@example.com`,
    passwordHash,
    name: 'Admin User',
    role: UserRole.ADMIN,
  });
  adminUserId = adminUser.id;
  adminToken = app.jwt.sign({ sub: adminUser.id, role: adminUser.role }, { expiresIn: '1h' });

  const taUser = await createUser({
    email: `ta_audit_${crypto.randomUUID()}@example.com`,
    passwordHash,
    name: 'TA User',
    role: UserRole.TA,
  });
  taUserId = taUser.id;
  taToken = app.jwt.sign({ sub: taUser.id, role: taUser.role }, { expiresIn: '1h' });

  const studentUser = await createUser({
    email: `student_audit_${crypto.randomUUID()}@example.com`,
    passwordHash,
    name: 'Student User',
    role: UserRole.STUDENT,
  });
  studentUserId = studentUser.id;
  studentToken = app.jwt.sign({ sub: studentUser.id, role: studentUser.role }, { expiresIn: '1h' });

  const facultyUser = await createUser({
    email: `faculty_audit_${crypto.randomUUID()}@example.com`,
    passwordHash,
    name: 'Faculty User',
    role: UserRole.FACULTY,
  });
  facultyUserId = facultyUser.id;
  facultyToken = app.jwt.sign({ sub: facultyUser.id, role: facultyUser.role }, { expiresIn: '1h' });
});

afterAll(async () => {
  await deleteAllData();
  await deleteUsers([adminUserId, taUserId, studentUserId, facultyUserId].filter(Boolean));
  await app.close();
});

describe('Audit Logging', () => {
  describe('Component Audit Logs', () => {
    test('logs CREATE action when component is created', async () => {
      const response = await app.inject({
        method: 'POST',
        url: '/components',
        headers: { authorization: `Bearer ${adminToken}` },
        payload: {
          name: 'Audit Test Component',
          description: 'For audit testing',
          totalQuantity: 10,
          category: ComponentCategory.Sensors,
          location: 'IoT Lab',
        },
      });

      assert.equal(response.statusCode, 201);
      const created = response.json().component;

      // Check audit log was created
      const logs = await getAuditLogs({ entityType: 'Component', entityId: created.id });
      assert.equal(logs.length, 1);
      assert.equal(logs[0].action, AuditActionType.CREATE);
      assert.equal(logs[0].userId, adminUserId);
      assert.equal(logs[0].entityType, 'Component');
      assert.equal(logs[0].entityId, created.id);
      assert.ok(logs[0].newValues);
      const newValues = JSON.parse(logs[0].newValues!);
      assert.equal(newValues.name, 'Audit Test Component');

      await db.delete(auditLog).where(eq(auditLog.entityId, created.id));
      await db.delete(component).where(eq(component.id, created.id));
    });

    test('logs UPDATE action when component is updated', async () => {
      const component = await createComponent({
        name: 'Component to Update',
        description: 'Original description',
        totalQuantity: 5,
        availableQuantity: 5,
        category: ComponentCategory.Sensors,
        location: Location.IoT_Lab,
      });

      const response = await app.inject({
        method: 'PUT',
        url: `/components/${component.id}`,
        headers: { authorization: `Bearer ${taToken}` },
        payload: {
          name: 'Updated Component Name',
          totalQuantity: 15,
        },
      });

      assert.equal(response.statusCode, 200);

      // Check audit log was created
      const logs = await getAuditLogs({ entityType: 'Component', entityId: component.id });
      const updateLog = logs.find((l) => l.action === AuditActionType.UPDATE);
      assert.ok(updateLog, 'UPDATE audit log should exist');
      assert.equal(updateLog!.userId, taUserId);
      assert.ok(updateLog!.oldValues);
      assert.ok(updateLog!.newValues);

      const oldValues = JSON.parse(updateLog!.oldValues!);
      const newValues = JSON.parse(updateLog!.newValues!);
      assert.equal(oldValues.name, 'Component to Update');
      assert.equal(newValues.name, 'Updated Component Name');
      assert.equal(oldValues.totalQuantity, 5);
      assert.equal(newValues.totalQuantity, 15);

      await deleteAllData();
    });

    test('logs DELETE action when component is deleted', async () => {
      const component = await createComponent({
        name: 'Component to Delete',
        totalQuantity: 5,
        availableQuantity: 5,
      });

      const response = await app.inject({
        method: 'DELETE',
        url: `/components/${component.id}`,
        headers: { authorization: `Bearer ${adminToken}` },
      });

      assert.equal(response.statusCode, 204);

      // Check audit log was created
      const logs = await getAuditLogs({ entityType: 'Component', entityId: component.id });
      const deleteLog = logs.find((l) => l.action === AuditActionType.DELETE);
      assert.ok(deleteLog, 'DELETE audit log should exist');
      assert.equal(deleteLog!.userId, adminUserId);
      assert.equal(deleteLog!.entityType, 'Component');
      assert.equal(deleteLog!.entityId, component.id);
      assert.ok(deleteLog!.oldValues);
      const oldValues = JSON.parse(deleteLog!.oldValues!);
      assert.equal(oldValues.name, 'Component to Delete');

      await deleteAllData();
    });
  });

  describe('Authentication Audit Logs', () => {
    test('logs LOGIN action on successful password login', async () => {
      const { hash } = await import('bcryptjs');
      const passwordHash = await hash('testpass123', 12);
      const testUser = await createUser({
        email: `logintest_${crypto.randomUUID()}@example.com`,
        passwordHash,
        name: 'Login Test User',
        role: UserRole.ADMIN,
      });

      const response = await app.inject({
        method: 'POST',
        url: '/auth/login',
        payload: {
          email: testUser.email,
          password: 'testpass123',
        },
      });

      assert.equal(response.statusCode, 200);

      // Check audit log was created
      const logs = await db
        .select()
        .from(auditLog)
        .where(and(eq(auditLog.userId, testUser.id), eq(auditLog.action, AuditActionType.LOGIN)));

      assert.equal(logs.length, 1);
      assert.equal(logs[0].action, AuditActionType.LOGIN);
      assert.ok(logs[0].metadata);
      const metadata = JSON.parse(logs[0].metadata!);
      assert.equal(metadata.method, 'password');

      await db.delete(auditLog).where(eq(auditLog.userId, testUser.id));
      await deleteUsers([testUser.id]);
    });

    test('captures IP address and user agent in audit logs', async () => {
      const component = await createComponent({
        name: 'Component for IP Test',
        totalQuantity: 5,
        availableQuantity: 5,
      });

      const response = await app.inject({
        method: 'DELETE',
        url: `/components/${component.id}`,
        headers: {
          authorization: `Bearer ${adminToken}`,
          'user-agent': 'Test-Agent/1.0',
        },
      });

      assert.equal(response.statusCode, 204);

      // Check audit log captured IP and user agent
      const logs = await getAuditLogs({ entityType: 'Component', entityId: component.id });
      const deleteLog = logs.find((l) => l.action === AuditActionType.DELETE);
      assert.ok(deleteLog, 'DELETE audit log should exist');
      assert.ok(deleteLog!.ipAddress, 'IP address should be captured');
      assert.equal(deleteLog!.userAgent, 'Test-Agent/1.0');

      await deleteAllData();
    });
  });

  describe('Request Audit Logs', () => {
    test('logs CREATE action when request is created', async () => {
      const component = await createComponent({
        name: 'Request Component',
        totalQuantity: 10,
        availableQuantity: 10,
      });

      const response = await app.inject({
        method: 'POST',
        url: '/requests',
        headers: { authorization: `Bearer ${studentToken}` },
        payload: {
          items: [{ componentId: component.id, quantity: 2 }],
          targetFacultyId: facultyUserId,
          projectTitle: 'Audit Test Project',
        },
      });

      assert.equal(response.statusCode, 201);
      const createdRequest = response.json().request;

      // Check audit log was created
      const logs = await getAuditLogs({ entityType: 'Request', entityId: createdRequest.id });
      assert.equal(logs.length, 1);
      assert.equal(logs[0].action, AuditActionType.CREATE);
      assert.equal(logs[0].userId, studentUserId);
      assert.equal(logs[0].entityType, 'Request');
      assert.ok(logs[0].newValues);
      const newValues = JSON.parse(logs[0].newValues!);
      assert.equal(newValues.projectTitle, 'Audit Test Project');

      await deleteAllData();
    });

    test('logs DELETE action when request is retracted', async () => {
      const component = await createComponent({
        name: 'Request Component',
        totalQuantity: 10,
        availableQuantity: 10,
      });

      const createResponse = await app.inject({
        method: 'POST',
        url: '/requests',
        headers: { authorization: `Bearer ${studentToken}` },
        payload: {
          items: [{ componentId: component.id, quantity: 1 }],
          targetFacultyId: facultyUserId,
          projectTitle: 'Delete Test Project',
        },
      });

      const createdRequest = createResponse.json().request;

      const deleteResponse = await app.inject({
        method: 'DELETE',
        url: `/requests/${createdRequest.id}`,
        headers: { authorization: `Bearer ${studentToken}` },
      });

      assert.equal(deleteResponse.statusCode, 204);

      // Check audit log was created
      const logs = await getAuditLogs({ entityType: 'Request', entityId: createdRequest.id });
      const deleteLog = logs.find((l) => l.action === AuditActionType.DELETE);
      assert.ok(deleteLog, 'DELETE audit log should exist');
      assert.equal(deleteLog!.userId, studentUserId);
      assert.ok(deleteLog!.oldValues);
      const oldValues = JSON.parse(deleteLog!.oldValues!);
      assert.equal(oldValues.status, 'PENDING');

      await deleteAllData();
    });
  });
});
