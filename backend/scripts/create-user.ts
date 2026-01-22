import 'dotenv/config';
import { PrismaClient, UserRole } from '@prisma/client';
import { PrismaPg } from '@prisma/adapter-pg';
import pg from 'pg';
import { hash } from 'bcryptjs';

const { Pool } = pg;

function getArg(name: string): string | undefined {
  const idx = process.argv.findIndex((a) => a === name || a.startsWith(`${name}=`));
  if (idx === -1) return undefined;
  const arg = process.argv[idx]!;
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
      'Usage: pnpm create:user -- --email someone@example.com --password secret --role STUDENT|FACULTY|TA|ADMIN [--name "Full Name"]',
    );
    process.exit(1);
  }

  if (!Object.values(UserRole).includes(roleRaw as UserRole)) {
    console.error('Invalid --role. Must be one of:', Object.values(UserRole).join(', '));
    process.exit(1);
  }

  const databaseUrl = process.env.DATABASE_URL;
  if (!databaseUrl) {
    console.error('DATABASE_URL is required');
    process.exit(1);
  }

  const pool = new Pool({ connectionString: databaseUrl });
  const adapter = new PrismaPg(pool);
  const prisma = new PrismaClient({ adapter });

  try {
    const existing = await prisma.user.findUnique({ where: { email } });
    if (existing) {
      console.error(`User already exists: ${email} (${existing.id})`);
      process.exit(1);
    }

    const passwordHash = await hash(password, 12);
    const user = await prisma.user.create({
      data: {
        email,
        passwordHash,
        name,
        role: roleRaw as UserRole,
      },
      select: {
        id: true,
        email: true,
        name: true,
        role: true,
        createdAt: true,
      },
    });

    console.log('✅ Created user:');
    console.log(JSON.stringify(user, null, 2));
  } finally {
    await prisma.$disconnect();
    await pool.end();
  }
}

main().catch((e) => {
  console.error('❌ Error creating user:', e);
  process.exit(1);
});
