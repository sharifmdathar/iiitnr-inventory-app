import 'dotenv/config';
import { eq } from 'drizzle-orm';
import { hash } from 'bcryptjs';
import { db, pool } from '../src/drizzle/db.js';
import { user } from '../src/drizzle/schema.js';
import { UserRole } from '../src/utils/enums.js';

function getArg(name: string): string | undefined {
  const idx = process.argv.findIndex((a) => a === name || a.startsWith(`${name}=`));
  if (idx === -1) return undefined;
  const arg = process.argv[idx];
  if (!arg) return undefined;
  if (arg.includes('=')) return arg.split('=')[1];
  return process.argv[idx + 1];
}

async function main() {
  const email = getArg('--email')?.trim();
  const password = getArg('--password');
  const name = getArg('--name')?.trim() || null;
  const roleRaw = getArg('--role')?.trim()?.toUpperCase();

  if (!email || !password || !roleRaw) {
    console.error(
      'Usage: bun create:user -- --email someone@example.com --password secret --role PENDING|STUDENT|FACULTY|TA|ADMIN [--name "Full Name"]',
    );
    process.exitCode = 1;
    return;
  }

  if (!Object.values(UserRole).includes(roleRaw as (typeof UserRole)[keyof typeof UserRole])) {
    console.error('Invalid --role. Must be one of:', Object.values(UserRole).join(', '));
    process.exitCode = 1;
    return;
  }

  try {
    const [existing] = await db.select().from(user).where(eq(user.email, email)).limit(1);

    if (existing) {
      console.error(`User already exists: ${email} (${existing.id})`);
      process.exitCode = 1;
      return;
    }

    const passwordHash = await hash(password, 12);
    const now = new Date().toISOString();

    const [created] = await db
      .insert(user)
      .values({
        id: crypto.randomUUID(),
        email,
        passwordHash,
        name,
        role: roleRaw as (typeof UserRole)[keyof typeof UserRole],
        createdAt: now,
        updatedAt: now,
      })
      .returning({
        id: user.id,
        email: user.email,
        name: user.name,
        role: user.role,
        createdAt: user.createdAt,
      });

    if (created) {
      console.log('✅ Created user:');
      console.log(JSON.stringify(created, null, 2));
    }
  } finally {
    await pool.end();
  }
}

main().catch((e) => {
  console.error('❌ Error creating user:', e);
  process.exitCode = 1;
});
