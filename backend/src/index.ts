import 'dotenv/config';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { migrate } from 'drizzle-orm/node-postgres/migrator';
import { applyNonTransactionalMigrations } from './drizzle/non-transactional-migrations.js';
import { db, pool } from './drizzle/db.js';

const __dirname = dirname(fileURLToPath(import.meta.url));
const migrationsFolder = process.env.MIGRATIONS_FOLDER ?? join(__dirname, 'drizzle');

(async () => {
  try {
    // Drizzle's migrate() wraps every pending migration in a single transaction
    // (see PgDialect.migrate -> session.transaction). ALTER TYPE ... ADD VALUE
    // run inside a transaction does not make the new enum label reliably visible
    // to pooled callers immediately after commit.
    //
    // 0015_brisk_kitten_mori.sql (ALTER TYPE ... ADD VALUE) IS in the drizzle-kit
    // journal, so migrate() applies it inside its transaction. The new enum
    // value is not reliably visible to pooled callers after that transaction
    // commits, so we re-apply it non-transactionally from the same pool
    // after migrate() completes. The 0015 SQL uses IF NOT EXISTS so it is
    // idempotent: safe to run even if migrate() already applied it.

    // 1) Apply all migrations with the ORM migrate helper.
    await migrate(db, { migrationsFolder });
    console.log('ORM migrations applied');

    // 2) Re-apply the enum migration non-transactionally so its value is
    //    visible to pooled callers immediately. The shared helper (in
    //    src/drizzle/non-transactional-migrations.ts) is called by both the
    //    production bootstrap and the test DB prepare step.
    await applyNonTransactionalMigrations(pool, migrationsFolder);

    console.log('Migrations complete');
  } catch (err) {
    throw new Error(`Migration failed: ${err}`, { cause: err });
  } finally {
    await pool.end();
  }
})().catch((err) => {
  console.error(err);
  process.exit(1);
});
