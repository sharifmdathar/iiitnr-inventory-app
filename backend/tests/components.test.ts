import './setup.js';

import { describe, test, beforeAll, afterAll } from 'bun:test';
import assert from 'node:assert/strict';
import { buildApp } from '../src/app.js';
import { prisma } from '../src/lib/prisma.js';
import { UserRole, ComponentCategory, Location } from '@prisma/client';

let app: Awaited<ReturnType<typeof buildApp>>;
let adminToken: string;
let taToken: string;
let studentToken: string;
let facultyToken: string;
let adminUserId: string;
let taUserId: string;
let studentUserId: string;
let facultyUserId: string;

beforeAll(async () => {
  app = await buildApp();

  await prisma.requestItem.deleteMany({});
  await prisma.request.deleteMany({});
  await prisma.component.deleteMany({});

  // Create test users with different roles
  const adminEmail = `admin_${Date.now()}@example.com`;
  const taEmail = `ta_${Date.now()}@example.com`;
  const studentEmail = `student_${Date.now()}@example.com`;
  const facultyEmail = `faculty_${Date.now()}@example.com`;

  // Import hash for password hashing
  const { hash } = await import('bcryptjs');
  const passwordHash = await hash('password123', 12);

  // Create ADMIN user directly in database (since registration blocks ADMIN role)
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

  // Register other roles through API
  const taResponse = await app.inject({
    method: 'POST',
    url: '/auth/register',
    payload: {
      email: taEmail,
      password: 'password123',
      name: 'TA User',
      role: UserRole.TA,
    },
  });
  taToken = taResponse.json().token;
  taUserId = taResponse.json().user.id;

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
  studentUserId = studentResponse.json().user.id;

  const facultyResponse = await app.inject({
    method: 'POST',
    url: '/auth/register',
    payload: {
      email: facultyEmail,
      password: 'password123',
      name: 'Faculty User',
      role: UserRole.FACULTY,
    },
  });
  facultyToken = facultyResponse.json().token;
  facultyUserId = facultyResponse.json().user.id;
});

afterAll(async () => {
  // Clean up test data
  await prisma.component.deleteMany({});
  const userIds = [adminUserId, taUserId, studentUserId, facultyUserId].filter(Boolean);
  if (userIds.length > 0) {
    await prisma.user.deleteMany({ where: { id: { in: userIds } } });
  }
  await app.close();
});

describe('Component CRUD API', () => {
  describe('GET /components - List all components', () => {
    test('returns 401 without token', async () => {
      const response = await app.inject({
        method: 'GET',
        url: '/components',
      });

      assert.equal(response.statusCode, 401);
    });

    test('returns 200 for STUDENT role', async () => {
      const response = await app.inject({
        method: 'GET',
        url: '/components',
        headers: {
          authorization: `Bearer ${studentToken}`,
        },
      });

      assert.equal(response.statusCode, 200);
      const body = response.json();
      assert.ok(Array.isArray(body.components));
    });

    test('returns 200 for FACULTY role', async () => {
      const response = await app.inject({
        method: 'GET',
        url: '/components',
        headers: {
          authorization: `Bearer ${facultyToken}`,
        },
      });

      assert.equal(response.statusCode, 200);
      const body = response.json();
      assert.ok(Array.isArray(body.components));
    });

    test('returns empty array when no components exist (ADMIN)', async () => {
      const response = await app.inject({
        method: 'GET',
        url: '/components',
        headers: {
          authorization: `Bearer ${adminToken}`,
        },
      });

      assert.equal(response.statusCode, 200);
      const body = response.json();
      assert.ok(Array.isArray(body.components));
      assert.equal(body.components.length, 0);
    });

    test('returns all components (TA)', async () => {
      // Create test components
      const component1 = await prisma.component.create({
        data: {
          name: 'Resistor 10k',
          description: '10k ohm resistor',
          totalQuantity: 50,
          availableQuantity: 50,
          category: ComponentCategory.Sensors,
          location: Location.IoT_Lab,
        },
      });

      const component2 = await prisma.component.create({
        data: {
          name: 'Arduino Uno',
          description: 'Arduino microcontroller',
          totalQuantity: 10,
          availableQuantity: 10,
          category: ComponentCategory.Microcontrollers,
          location: Location.Robo_Lab,
        },
      });

      const response = await app.inject({
        method: 'GET',
        url: '/components',
        headers: {
          authorization: `Bearer ${taToken}`,
        },
      });

      assert.equal(response.statusCode, 200);
      const body = response.json();
      assert.ok(Array.isArray(body.components));
      assert.equal(body.components.length, 2);

      // Components should be ordered by createdAt desc
      assert.equal(body.components[0].id, component2.id);
      assert.equal(body.components[1].id, component1.id);

      // Clean up
      await prisma.component.deleteMany({});
    });
  });

  describe('GET /components/:id - Get single component', () => {
    test('returns 401 without token', async () => {
      const response = await app.inject({
        method: 'GET',
        url: '/components/test-id',
      });

      assert.equal(response.statusCode, 401);
    });

    test('returns component by id (STUDENT)', async () => {
      const component = await prisma.component.create({
        data: {
          name: 'Student View Component',
          description: 'Visible to all roles',
          totalQuantity: 3,
          availableQuantity: 3,
        },
      });

      const response = await app.inject({
        method: 'GET',
        url: `/components/${component.id}`,
        headers: {
          authorization: `Bearer ${studentToken}`,
        },
      });

      assert.equal(response.statusCode, 200);
      const body = response.json();
      assert.equal(body.component.id, component.id);

      await prisma.component.deleteMany({});
    });

    test('returns 404 for non-existent component', async () => {
      const response = await app.inject({
        method: 'GET',
        url: '/components/non-existent-id',
        headers: {
          authorization: `Bearer ${adminToken}`,
        },
      });

      assert.equal(response.statusCode, 404);
      const body = response.json();
      assert.ok(body.error.includes('not found'));
    });

    test('returns component by id (ADMIN)', async () => {
      const component = await prisma.component.create({
        data: {
          name: 'Test Component',
          description: 'Test description',
          totalQuantity: 5,
          availableQuantity: 5,
          category: ComponentCategory.Actuators,
          location: Location.VLSI_Lab,
        },
      });

      const response = await app.inject({
        method: 'GET',
        url: `/components/${component.id}`,
        headers: {
          authorization: `Bearer ${adminToken}`,
        },
      });

      assert.equal(response.statusCode, 200);
      const body = response.json();
      assert.equal(body.component.id, component.id);
      assert.equal(body.component.name, 'Test Component');
      assert.equal(body.component.description, 'Test description');
      assert.equal(body.component.totalQuantity, 5);
      assert.equal(body.component.availableQuantity, 5);
      assert.equal(body.component.category, ComponentCategory.Actuators);
      assert.equal(body.component.location, Location.VLSI_Lab);

      await prisma.component.deleteMany({});
    });
  });

  describe('POST /components - Create component', () => {
    test('returns 401 without token', async () => {
      const response = await app.inject({
        method: 'POST',
        url: '/components',
        payload: {
          name: 'Test Component',
          totalQuantity: 10,
        },
      });

      assert.equal(response.statusCode, 401);
    });

    test('returns 403 for STUDENT role', async () => {
      const response = await app.inject({
        method: 'POST',
        url: '/components',
        headers: {
          authorization: `Bearer ${studentToken}`,
        },
        payload: {
          name: 'Test Component',
          quantity: 10,
        },
      });

      assert.equal(response.statusCode, 403);
    });

    test('returns 400 when name is missing', async () => {
      const response = await app.inject({
        method: 'POST',
        url: '/components',
        headers: {
          authorization: `Bearer ${adminToken}`,
        },
        payload: {
          quantity: 10,
        },
      });

      assert.equal(response.statusCode, 400);
      const body = response.json();
      assert.ok(body.error.includes('name is required'));
    });

    test('returns 400 when quantity is negative', async () => {
      const response = await app.inject({
        method: 'POST',
        url: '/components',
        headers: {
          authorization: `Bearer ${adminToken}`,
        },
        payload: {
          name: 'Test Component',
          totalQuantity: -1,
        },
      });

      assert.equal(response.statusCode, 400);
      const body = response.json();
      assert.ok(body.error.includes('totalQuantity'));
    });

    test('creates component with all fields (TA)', async () => {
      const response = await app.inject({
        method: 'POST',
        url: '/components',
        headers: {
          authorization: `Bearer ${taToken}`,
        },
        payload: {
          name: 'New Component',
          description: 'Component description',
          totalQuantity: 25,
          category: ComponentCategory.Sensors,
          location: 'IoT Lab',
        },
      });

      assert.equal(response.statusCode, 201);
      const body = response.json();
      assert.ok(body.component.id);
      assert.equal(body.component.name, 'New Component');
      assert.equal(body.component.description, 'Component description');
      assert.equal(body.component.totalQuantity, 25);
      assert.equal(body.component.availableQuantity, 25);
      assert.equal(body.component.category, ComponentCategory.Sensors);
      assert.equal(body.component.location, Location.IoT_Lab);
      assert.ok(body.component.createdAt);
      assert.ok(body.component.updatedAt);

      await prisma.component.deleteMany({ where: { id: body.component.id } });
    });

    test('creates component with minimal fields (ADMIN)', async () => {
      const response = await app.inject({
        method: 'POST',
        url: '/components',
        headers: {
          authorization: `Bearer ${adminToken}`,
        },
        payload: {
          name: 'Minimal Component',
        },
      });

      assert.equal(response.statusCode, 201);
      const body = response.json();
      assert.ok(body.component.id);
      assert.equal(body.component.name, 'Minimal Component');
      assert.equal(body.component.totalQuantity, 0);
      assert.equal(body.component.availableQuantity, 0);
      assert.equal(body.component.description, null);
      assert.equal(body.component.category, null);
      assert.equal(body.component.location, null);

      await prisma.component.deleteMany({ where: { id: body.component.id } });
    });
  });

  describe('PUT /components/:id - Update component', () => {
    test('returns 401 without token', async () => {
      const response = await app.inject({
        method: 'PUT',
        url: '/components/test-id',
        payload: {
          name: 'Updated Component',
        },
      });

      assert.equal(response.statusCode, 401);
    });

    test('returns 403 for FACULTY role', async () => {
      const response = await app.inject({
        method: 'PUT',
        url: '/components/test-id',
        headers: {
          authorization: `Bearer ${facultyToken}`,
        },
        payload: {
          name: 'Updated Component',
        },
      });

      assert.equal(response.statusCode, 403);
    });

    test('returns 404 for non-existent component', async () => {
      const response = await app.inject({
        method: 'PUT',
        url: '/components/non-existent-id',
        headers: {
          authorization: `Bearer ${adminToken}`,
        },
        payload: {
          name: 'Updated Component',
        },
      });

      assert.equal(response.statusCode, 404);
      const body = response.json();
      assert.ok(body.error.includes('not found'));
    });

    test('returns 400 when quantity is negative', async () => {
      const component = await prisma.component.create({
        data: {
          name: 'Test Component',
          totalQuantity: 10,
          availableQuantity: 10,
        },
      });

      const response = await app.inject({
        method: 'PUT',
        url: `/components/${component.id}`,
        headers: {
          authorization: `Bearer ${adminToken}`,
        },
        payload: {
          totalQuantity: -5,
        },
      });

      assert.equal(response.statusCode, 400);
      const body = response.json();
      assert.ok(body.error.includes('totalQuantity'));

      await prisma.component.deleteMany({});
    });

    test('updates component partially (TA)', async () => {
      const component = await prisma.component.create({
        data: {
          name: 'Original Name',
          description: 'Original description',
          totalQuantity: 10,
          availableQuantity: 10,
          category: ComponentCategory.Sensors,
          location: Location.IoT_Lab,
        },
      });

      const response = await app.inject({
        method: 'PUT',
        url: `/components/${component.id}`,
        headers: {
          authorization: `Bearer ${taToken}`,
        },
        payload: {
          name: 'Updated Name',
          totalQuantity: 20,
        },
      });

      assert.equal(response.statusCode, 200);
      const body = response.json();
      assert.equal(body.component.id, component.id);
      assert.equal(body.component.name, 'Updated Name');
      assert.equal(body.component.description, 'Original description'); // unchanged
      assert.equal(body.component.totalQuantity, 20);
      assert.equal(body.component.availableQuantity, 20);
      assert.equal(body.component.category, ComponentCategory.Sensors); // unchanged
      assert.equal(body.component.location, Location.IoT_Lab); // unchanged

      await prisma.component.deleteMany({});
    });

    test('updates all fields (ADMIN)', async () => {
      const component = await prisma.component.create({
        data: {
          name: 'Original Name',
          description: 'Original description',
          totalQuantity: 10,
          availableQuantity: 10,
          category: ComponentCategory.Sensors,
          location: Location.IoT_Lab,
        },
      });

      const response = await app.inject({
        method: 'PUT',
        url: `/components/${component.id}`,
        headers: {
          authorization: `Bearer ${adminToken}`,
        },
        payload: {
          name: 'Fully Updated Name',
          description: 'Fully updated description',
          totalQuantity: 30,
          category: ComponentCategory.Actuators,
          location: 'Robo Lab',
        },
      });

      assert.equal(response.statusCode, 200);
      const body = response.json();
      assert.equal(body.component.name, 'Fully Updated Name');
      assert.equal(body.component.description, 'Fully updated description');
      assert.equal(body.component.totalQuantity, 30);
      assert.equal(body.component.availableQuantity, 30);
      assert.equal(body.component.category, ComponentCategory.Actuators);
      assert.equal(body.component.location, Location.Robo_Lab);

      await prisma.component.deleteMany({});
    });

    test('can set optional fields to null (TA)', async () => {
      const component = await prisma.component.create({
        data: {
          name: 'Test Component',
          description: 'Has description',
          category: ComponentCategory.Microcontrollers,
          location: Location.VLSI_Lab,
          totalQuantity: 10,
          availableQuantity: 10,
        },
      });

      const response = await app.inject({
        method: 'PUT',
        url: `/components/${component.id}`,
        headers: {
          authorization: `Bearer ${taToken}`,
        },
        payload: {
          description: '',
          category: '',
          location: '',
        },
      });

      assert.equal(response.statusCode, 200);
      const body = response.json();
      assert.equal(body.component.description, null);
      assert.equal(body.component.category, null);
      assert.equal(body.component.location, null);

      await prisma.component.deleteMany({});
    });
  });

  describe('DELETE /components/:id - Delete component', () => {
    test('returns 401 without token', async () => {
      const response = await app.inject({
        method: 'DELETE',
        url: '/components/test-id',
      });

      assert.equal(response.statusCode, 401);
    });

    test('returns 403 for STUDENT role', async () => {
      const response = await app.inject({
        method: 'DELETE',
        url: '/components/test-id',
        headers: {
          authorization: `Bearer ${studentToken}`,
        },
      });

      assert.equal(response.statusCode, 403);
    });

    test('returns 404 for non-existent component', async () => {
      const response = await app.inject({
        method: 'DELETE',
        url: '/components/non-existent-id',
        headers: {
          authorization: `Bearer ${adminToken}`,
        },
      });

      assert.equal(response.statusCode, 404);
      const body = response.json();
      assert.ok(body.error.includes('not found'));
    });

    test('deletes component successfully (ADMIN)', async () => {
      const component = await prisma.component.create({
        data: {
          name: 'Component to Delete',
          totalQuantity: 5,
          availableQuantity: 5,
        },
      });

      const response = await app.inject({
        method: 'DELETE',
        url: `/components/${component.id}`,
        headers: {
          authorization: `Bearer ${adminToken}`,
        },
      });

      assert.equal(response.statusCode, 204);

      // Verify component is deleted
      const deletedComponent = await prisma.component.findUnique({
        where: { id: component.id },
      });
      assert.equal(deletedComponent, null);
    });

    test('deletes component successfully (TA)', async () => {
      const component = await prisma.component.create({
        data: {
          name: 'Component to Delete',
          totalQuantity: 5,
          availableQuantity: 5,
        },
      });

      const response = await app.inject({
        method: 'DELETE',
        url: `/components/${component.id}`,
        headers: {
          authorization: `Bearer ${taToken}`,
        },
      });

      assert.equal(response.statusCode, 204);

      const deletedComponent = await prisma.component.findUnique({
        where: { id: component.id },
      });
      assert.equal(deletedComponent, null);
    });

    test('DELETE request with form-urlencoded content-type is accepted', async () => {
      const component = await prisma.component.create({
        data: {
          name: 'Component to Delete with Form',
          totalQuantity: 5,
          availableQuantity: 5,
        },
      });

      const response = await app.inject({
        method: 'DELETE',
        url: `/components/${component.id}`,
        headers: {
          authorization: `Bearer ${adminToken}`,
          'content-type': 'application/x-www-form-urlencoded',
        },
        payload: '',
      });

      assert.equal(response.statusCode, 204);

      // Verify component is deleted
      const deletedComponent = await prisma.component.findUnique({
        where: { id: component.id },
      });
      assert.equal(deletedComponent, null);
    });
  });

  describe('Integration tests', () => {
    test('Full CRUD workflow - create, read, update, delete (ADMIN)', async () => {
      // Create
      const createResponse = await app.inject({
        method: 'POST',
        url: '/components',
        headers: {
          authorization: `Bearer ${adminToken}`,
        },
        payload: {
          name: 'Workflow Component',
          description: 'Testing full workflow',
          quantity: 15,
          category: ComponentCategory.Sensors,
          location: 'VLSI Lab',
        },
      });

      assert.equal(createResponse.statusCode, 201);
      const created = createResponse.json().component;
      assert.ok(created.id);

      // Read
      const readResponse = await app.inject({
        method: 'GET',
        url: `/components/${created.id}`,
        headers: {
          authorization: `Bearer ${adminToken}`,
        },
      });

      assert.equal(readResponse.statusCode, 200);
      assert.equal(readResponse.json().component.name, 'Workflow Component');

      // Update
      const updateResponse = await app.inject({
        method: 'PUT',
        url: `/components/${created.id}`,
        headers: {
          authorization: `Bearer ${adminToken}`,
        },
        payload: {
          totalQuantity: 25,
        },
      });

      assert.equal(updateResponse.statusCode, 200);
      assert.equal(updateResponse.json().component.totalQuantity, 25);
      assert.equal(updateResponse.json().component.availableQuantity, 25);

      // Delete
      const deleteResponse = await app.inject({
        method: 'DELETE',
        url: `/components/${created.id}`,
        headers: {
          authorization: `Bearer ${adminToken}`,
        },
      });

      assert.equal(deleteResponse.statusCode, 204);
    });
  });
});
