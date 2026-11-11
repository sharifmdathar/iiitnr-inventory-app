import 'dotenv/config';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { Pool } from 'pg';
import { migrate } from 'drizzle-orm/node-postgres/migrator';
import { applyNonTransactionalMigrations } from '../src/drizzle/non-transactional-migrations.js';
import { db, pool } from '../src/drizzle/db.js';

const url = process.env.TEST_DATABASE_URL ?? process.env.DATABASE_URL;
if (!url) {
  throw new Error('TEST_DATABASE_URL (or DATABASE_URL) must be set to prepare the test database');
}

async function waitForDb(maxAttempts = 30): Promise<void> {
  for (let i = 0; i < maxAttempts; i++) {
    const p = new Pool({ connectionString: url, connectionTimeoutMillis: 2000, max: 1 });
    try {
      await p.query('SELECT 1');
      await p.end();
      return;
    } catch (err) {
      console.warn(`⚠️ DB not ready (attempt ${i + 1}/${maxAttempts}): ${(err as Error).message}`);
      await p.end().catch(() => {});
      await new Promise((r) => setTimeout(r, 500));
    }
  }
  throw new Error('Database not ready after 15 seconds');
}

const __dirname = dirname(fileURLToPath(import.meta.url));
const migrationsFolder = join(__dirname, '..', 'src', 'drizzle');

await waitForDb();

// Drizzle's migrate() wraps every pending migration in one transaction (see
// drizzle-orm/pg-core/dialect.js: PgDialect.migrate -> session.transaction).
// ALTER TYPE ... ADD VALUE run inside a transaction does not make the new enum
// label reliably visible to pooled callers immediately after commit.
//
// 0015_brisk_kitten_mori.sql (ALTER TYPE ... ADD VALUE) IS in the drizzle-kit
// journal (_journal.json), so migrate() applies it inside its transaction. The
// new enum value is not reliably visible to pooled callers after that
// transaction commits, so we re-apply it non-transactionally from the same pool
// after migrate() completes. The 0015 SQL uses IF NOT EXISTS so it is
// idempotent: safe to run even if migrate() already applied it.

// 1) Apply all migrations with the ORM migrate helper (transactional).
await migrate(db, { migrationsFolder });
console.log('ORM migrations applied');

// 2) Re-apply the enum migration non-transactionally so its value is visible
//    to pooled callers immediately. The shared helper lives in
//    src/drizzle/non-transactional-migrations.ts and is the single source of
//    truth for which migrations need this treatment.
await applyNonTransactionalMigrations(pool, migrationsFolder);

// Truncate test data, leaving the schema intact.
const truncatePool = new Pool({ connectionString: url });
try {
  await truncatePool.query(
    'TRUNCATE TABLE "User", "Request", "Component", "RequestItem", "AuditLog", "Notification", "RequestEmail" RESTART IDENTITY CASCADE',
  );
} finally {
  await truncatePool.end();
}
await pool.end();
console.log('Test database prepared (migrate + enum migration + truncate)');
