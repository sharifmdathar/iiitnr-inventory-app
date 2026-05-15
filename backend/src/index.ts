import 'dotenv/config';
import { pool, db } from './drizzle/db.js';
import { join } from 'node:path';
import { migrate } from 'drizzle-orm/node-postgres/migrator';

const migrationsFolder = process.env.MIGRATIONS_FOLDER ?? join(import.meta.dir, 'drizzle');

(async () => {
  try {
    await migrate(db, { migrationsFolder });
    console.log('Migrations complete');
  } catch (err) {
    console.error('Migration failed:', err);
    process.exit(1);
  } finally {
    await pool.end();
  }
})();
