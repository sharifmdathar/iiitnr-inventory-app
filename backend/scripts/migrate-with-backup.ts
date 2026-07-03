import 'dotenv/config';
import { execSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';

const BACKUPS_DIR = path.join(process.cwd(), 'backups');

const APP_TABLES = [
  'public."Component"',
  'public."Request"',
  'public."RequestItem"',
  'public."User"',
];

function urlForMigrations(rawUrl: string): string {
  try {
    const url = new URL(rawUrl);
    const isSupabasePooler = url.hostname.includes('supabase') && url.port === '6543';
    if (isSupabasePooler) {
      url.port = '5432';
      console.log('   (Using Supabase session port 5432 for schema/migrate steps)');
      return url.toString();
    }
  } catch {}
  return rawUrl;
}

function run(cmd: string, description: string, env: NodeJS.ProcessEnv = process.env): void {
  console.log(`\n▶ ${description}...`);
  try {
    execSync(cmd, { stdio: 'inherit', env: { ...env }, shell: '/bin/bash' });
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err);
    console.error(`\n❌ ${description} failed: ${message}`);
    throw new Error(`${description} failed: ${message}`);
  }
}

function getArg(name: string): string | undefined {
  const argv = process.argv.slice(2);
  const prefix = `${name}=`;
  for (let i = 0; i < argv.length; i++) {
    const currentArg = argv[i];
    if (currentArg === undefined) continue;
    if (currentArg === name) return argv[i + 1];
    if (currentArg.startsWith(prefix)) return currentArg.slice(prefix.length);
  }
  return undefined;
}

function main(): void {
  const url = process.env.DATABASE_URL;
  if (!url || (!url.startsWith('postgresql') && !url.startsWith('postgres://'))) {
    throw new Error('❌ DATABASE_URL must be set and point to a PostgreSQL database.');
  }

  const restoreFrom = getArg('--restore-from');
  const restoreFile = restoreFrom
    ? path.isAbsolute(restoreFrom)
      ? restoreFrom
      : path.join(process.cwd(), restoreFrom)
    : null;
  if (restoreFile && !fs.existsSync(restoreFile)) {
    throw new Error(`❌ Restore file not found: ${restoreFile}`);
  }

  if (!fs.existsSync(BACKUPS_DIR)) {
    fs.mkdirSync(BACKUPS_DIR, { recursive: true });
  }

  const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19);
  const backupFile = path.join(BACKUPS_DIR, `data_${timestamp}.sql`);
  const dataToRestore = restoreFile ?? backupFile;

  console.log('📦 Migrate with backup');
  if (restoreFile) {
    console.log('   Restore from (custom):', dataToRestore);
  } else {
    console.log('   Backup file:', backupFile);
  }

  if (!restoreFile) {
    const tableArgs = APP_TABLES.map((t) => `-t '${t}'`).join(' ');
    run(
      `pg_dump "$DATABASE_URL" --data-only --no-owner --no-acl ${tableArgs} -f "${backupFile}"`,
      'Backing up app data (pg_dump)',
      process.env,
    );
  }

  const resetSql = [
    'DROP SCHEMA public CASCADE',
    'CREATE SCHEMA public',
    'GRANT ALL ON SCHEMA public TO public',
  ].join(';\n');
  const resetSqlFile = path.join(BACKUPS_DIR, `.reset_${timestamp}.sql`);
  fs.writeFileSync(resetSqlFile, resetSql, 'utf8');
  const migrationUrl = urlForMigrations(url);
  try {
    run(
      `psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f "${resetSqlFile}"`,
      'Dropping and recreating public schema',
      { ...process.env, DATABASE_URL: migrationUrl },
    );
  } finally {
    try {
      fs.unlinkSync(resetSqlFile);
    } catch {}
  }
  run('bun run migrate', 'Applying migrations (drizzle migrate)', {
    ...process.env,
    DATABASE_URL: migrationUrl,
  });

  run(
    `psql "$DATABASE_URL" -v ON_ERROR_STOP=0 -f "${dataToRestore}"`,
    'Restoring data (psql)',
    process.env,
  );

  console.log('\n✅ Done. Data backed up, migrations applied, data restored.');
}

main();
