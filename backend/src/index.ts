import 'dotenv/config';
import { pool, db } from './drizzle/db.js';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { dirname } from 'node:path';
import { migrate } from 'drizzle-orm/node-postgres/migrator';

const __dirname = dirname(fileURLToPath(import.meta.url));
const migrationsFolder = process.env.MIGRATIONS_FOLDER ?? join(__dirname, 'drizzle');

(async () => {
  try {
    await migrate(db, { migrationsFolder });
    console.log('Migrations complete');
  } catch (err) {
    throw new Error(`Migration failed: ${err}`, { cause: err });
  } finally {
    await pool.end();
  }
})();
