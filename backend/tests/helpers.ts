import { eq, inArray } from 'drizzle-orm';
import { db } from '../src/drizzle/db.js';
import { auditLog, component, request, requestItem, user } from '../src/drizzle/schema.js';
import type { RequestStatusValue } from '../src/utils/enums.js';
import { ComponentCategory, Location, UserRole } from '../src/utils/enums.js';

const now = () => new Date().toISOString();

export async function createUser(data: {
  email: string;
  passwordHash?: string;
  name?: string | null;
  role: (typeof UserRole)[keyof typeof UserRole];
  googleId?: string | null;
}) {
  const n = now();
  const [u] = await db
    .insert(user)
    .values({
      id: crypto.randomUUID(),
      email: data.email,
      passwordHash: data.passwordHash ?? null,
      name: data.name ?? null,
      role: data.role,
      googleId: data.googleId ?? null,
      createdAt: n,
      updatedAt: n,
    })
    .returning();
  return u!;
}

export async function createComponent(data: {
  name: string;
  description?: string | null;
  totalQuantity?: number;
  availableQuantity?: number;
  category?: (typeof ComponentCategory)[keyof typeof ComponentCategory] | null;
  location?: (typeof Location)[keyof typeof Location] | null;
}) {
  const n = now();
  const [c] = await db
    .insert(component)
    .values({
      id: crypto.randomUUID(),
      name: data.name,
      description: data.description ?? null,
      totalQuantity: data.totalQuantity ?? 0,
      availableQuantity: data.availableQuantity ?? data.totalQuantity ?? 0,
      category: data.category ?? null,
      location: data.location ?? null,
      createdAt: n,
      updatedAt: n,
    })
    .returning();
  return c!;
}

export async function createRequest(data: {
  userId: string;
  targetFacultyId: string;
  projectTitle: string;
  status?: RequestStatusValue;
  items: { componentId: string; quantity: number }[];
}) {
  const n = now();
  const requestId = crypto.randomUUID();
  await db.insert(request).values({
    id: requestId,
    userId: data.userId,
    targetFacultyId: data.targetFacultyId,
    projectTitle: data.projectTitle,
    status: data.status ?? 'PENDING',
    createdAt: n,
    updatedAt: n,
  });
  await db.insert(requestItem).values(
    data.items.map((item) => ({
      id: crypto.randomUUID(),
      requestId,
      componentId: item.componentId,
      quantity: item.quantity,
      createdAt: n,
      updatedAt: n,
    })),
  );
  const [r] = await db.select().from(request).where(eq(request.id, requestId)).limit(1);
  return r!;
}

export async function deleteAllData() {
  await db.delete(auditLog);
  await db.delete(requestItem);
  await db.delete(request);
  await db.delete(component);
}

export async function deleteUsers(ids: string[]) {
  if (ids.length > 0) {
    await db.delete(auditLog).where(inArray(auditLog.userId, ids));
    await db.delete(user).where(inArray(user.id, ids));
  }
}

export async function deleteComponents(ids: string[]) {
  if (ids.length > 0) {
    await db.delete(component).where(inArray(component.id, ids));
  }
}

export async function deleteAllRequests() {
  await db.delete(requestItem);
  await db.delete(request);
}

export async function findRequestById(id: string) {
  const [r] = await db.select().from(request).where(eq(request.id, id)).limit(1);
  return r ?? null;
}

export async function findComponentById(id: string) {
  const [c] = await db.select().from(component).where(eq(component.id, id)).limit(1);
  return c ?? null;
}

export { UserRole, ComponentCategory, Location };
