import 'dotenv/config';
import { pool, db } from './drizzle/db.js';
import { join } from 'node:path';
import { migrate } from 'drizzle-orm/node-postgres/migrator';

(async () => {
  try {
    await migrate(db, { migrationsFolder: join(import.meta.dir, 'drizzle') });
    console.log('Migrations complete');
  } finally {
    await pool.end();
  }
})();
