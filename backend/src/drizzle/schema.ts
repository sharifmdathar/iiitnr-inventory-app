import {
  pgTable,
  varchar,
  timestamp,
  text,
  integer,
  uniqueIndex,
  index,
  foreignKey,
  pgEnum,
} from 'drizzle-orm/pg-core';
import { sql } from 'drizzle-orm';

export const componentCategory = pgEnum('ComponentCategory', [
  'Sensors',
  'Actuators',
  'Microcontrollers',
  'Microprocessors',
  'Others',
]);
export const location = pgEnum('Location', ['IoT_Lab', 'Robo_Lab', 'VLSI_Lab']);
export const requestStatus = pgEnum('RequestStatus', [
  'PENDING',
  'APPROVED',
  'REJECTED',
  'FULFILLED',
  'RETURNED',
]);
export const userRole = pgEnum('UserRole', ['ADMIN', 'FACULTY', 'PENDING', 'STUDENT', 'TA']);

export const prismaMigrations = pgTable('_prisma_migrations', {
  id: varchar({ length: 36 }).primaryKey().notNull(),
  checksum: varchar({ length: 64 }).notNull(),
  finishedAt: timestamp('finished_at', { withTimezone: true, mode: 'string' }),
  migrationName: varchar('migration_name', { length: 255 }).notNull(),
  logs: text(),
  rolledBackAt: timestamp('rolled_back_at', { withTimezone: true, mode: 'string' }),
  startedAt: timestamp('started_at', { withTimezone: true, mode: 'string' }).defaultNow().notNull(),
  appliedStepsCount: integer('applied_steps_count').default(0).notNull(),
});

export const user = pgTable(
  'User',
  {
    id: text().primaryKey().notNull(),
    email: text().notNull(),
    name: text(),
    imageUrl: text(),
    passwordHash: text(),
    googleId: text(),
    role: userRole().default('PENDING').notNull(),
    createdAt: timestamp({ precision: 3, mode: 'string' })
      .default(sql`CURRENT_TIMESTAMP`)
      .notNull(),
    updatedAt: timestamp({ precision: 3, mode: 'string' }).notNull(),
  },
  (table) => [
    uniqueIndex('User_email_key').using('btree', table.email.asc().nullsLast().op('text_ops')),
    uniqueIndex('User_googleId_key').using(
      'btree',
      table.googleId.asc().nullsLast().op('text_ops'),
    ),
  ],
);

export const request = pgTable(
  'Request',
  {
    id: text().primaryKey().notNull(),
    userId: text().notNull(),
    targetFacultyId: text().notNull(),
    projectTitle: text().notNull(),
    status: requestStatus().default('PENDING').notNull(),
    createdAt: timestamp({ precision: 3, mode: 'string' })
      .default(sql`CURRENT_TIMESTAMP`)
      .notNull(),
    updatedAt: timestamp({ precision: 3, mode: 'string' }).notNull(),
  },
  (table) => [
    index('Request_targetFacultyId_idx').using(
      'btree',
      table.targetFacultyId.asc().nullsLast().op('text_ops'),
    ),
    index('Request_userId_idx').using('btree', table.userId.asc().nullsLast().op('text_ops')),
    foreignKey({
      columns: [table.targetFacultyId],
      foreignColumns: [user.id],
      name: 'Request_targetFacultyId_fkey',
    })
      .onUpdate('cascade')
      .onDelete('restrict'),
    foreignKey({
      columns: [table.userId],
      foreignColumns: [user.id],
      name: 'Request_userId_fkey',
    })
      .onUpdate('cascade')
      .onDelete('restrict'),
  ],
);

export const requestItem = pgTable(
  'RequestItem',
  {
    id: text().primaryKey().notNull(),
    requestId: text().notNull(),
    componentId: text().notNull(),
    quantity: integer().notNull(),
    createdAt: timestamp({ precision: 3, mode: 'string' })
      .default(sql`CURRENT_TIMESTAMP`)
      .notNull(),
    updatedAt: timestamp({ precision: 3, mode: 'string' }).notNull(),
  },
  (table) => [
    index('RequestItem_componentId_idx').using(
      'btree',
      table.componentId.asc().nullsLast().op('text_ops'),
    ),
    index('RequestItem_requestId_idx').using(
      'btree',
      table.requestId.asc().nullsLast().op('text_ops'),
    ),
    foreignKey({
      columns: [table.componentId],
      foreignColumns: [component.id],
      name: 'RequestItem_componentId_fkey',
    })
      .onUpdate('cascade')
      .onDelete('restrict'),
    foreignKey({
      columns: [table.requestId],
      foreignColumns: [request.id],
      name: 'RequestItem_requestId_fkey',
    })
      .onUpdate('cascade')
      .onDelete('restrict'),
  ],
);

export const component = pgTable('Component', {
  id: text().primaryKey().notNull(),
  name: text().notNull(),
  description: text(),
  imageUrl: text(),
  totalQuantity: integer().default(0).notNull(),
  availableQuantity: integer().default(0).notNull(),
  category: componentCategory(),
  location: location(),
  createdAt: timestamp({ precision: 3, mode: 'string' })
    .default(sql`CURRENT_TIMESTAMP`)
    .notNull(),
  updatedAt: timestamp({ precision: 3, mode: 'string' }).notNull(),
});
