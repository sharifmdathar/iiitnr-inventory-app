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
    const u = new URL(rawUrl);
    const isSupabasePooler = u.hostname.includes('supabase') && u.port === '6543';
    if (isSupabasePooler) {
      u.port = '5432';
      console.log('   (Using Supabase session port 5432 for schema/migrate steps)');
      return u.toString();
    }
  } catch {}
  return rawUrl;
}

function run(cmd: string, env: NodeJS.ProcessEnv = process.env, description: string): void {
  console.log(`\n▶ ${description}...`);
  try {
    execSync(cmd, {
      stdio: 'inherit',
      env: { ...env },
      shell: '/bin/bash',
    });
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err);
    console.error(`\n❌ ${description} failed: ${message}`);
    const stderr =
      err && typeof err === 'object' && 'stderr' in err && (err as { stderr?: unknown }).stderr;
    if (stderr != null && typeof stderr === 'string' && stderr.trim()) {
      console.error('\nStderr:', stderr.trim());
    }
    process.exit(1);
  }
}

function getArg(name: string): string | undefined {
  const argv = process.argv.slice(2);
  const prefix = name + '=';
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a === undefined) continue;
    if (a === name) return argv[i + 1];
    if (a.startsWith(prefix)) return a.slice(prefix.length);
  }
  return undefined;
}

function main(): void {
  const url = process.env.DATABASE_URL;
  if (!url || (!url.startsWith('postgresql') && !url.startsWith('postgres://'))) {
    console.error('❌ DATABASE_URL must be set and point to a PostgreSQL database.');
    process.exit(1);
  }

  const restoreFrom = getArg('--restore-from');
  const restoreFile = restoreFrom
    ? path.isAbsolute(restoreFrom)
      ? restoreFrom
      : path.join(process.cwd(), restoreFrom)
    : null;
  if (restoreFile && !fs.existsSync(restoreFile)) {
    console.error('❌ Restore file not found:', restoreFile);
    process.exit(1);
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
    const tableArgs = APP_TABLES.map((t) => "-t '" + t + "'").join(' ');
    run(
      'pg_dump "$DATABASE_URL" --data-only --no-owner --no-acl ' +
        tableArgs +
        ' -f "' +
        backupFile +
        '"',
      process.env,
      'Backing up app data (pg_dump)',
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
      'psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f "' + resetSqlFile + '"',
      { ...process.env, DATABASE_URL: migrationUrl },
      'Dropping and recreating public schema',
    );
  } finally {
    try {
      fs.unlinkSync(resetSqlFile);
    } catch {}
  }
  run(
    'bunx prisma migrate deploy',
    { ...process.env, DATABASE_URL: migrationUrl },
    'Applying migrations (prisma migrate deploy)',
  );

  run(
    'psql "$DATABASE_URL" -v ON_ERROR_STOP=0 -f "' + dataToRestore + '"',
    process.env,
    'Restoring data (psql)',
  );

  console.log('\n✅ Done. Data backed up, migrations applied, data restored.');
}

main();
