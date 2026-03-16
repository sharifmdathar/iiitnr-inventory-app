import 'dotenv/config';
import { hash } from 'bcryptjs';
import { eq } from 'drizzle-orm';
import { pool, db } from '../src/drizzle/db.js';
import { user } from '../src/drizzle/schema.js';
import { UserRole } from '../src/utils/enums.js';

async function main() {
  const adminEmail = process.env.ADMIN_EMAIL || 'admin@test.com';
  const adminPassword = process.env.ADMIN_PASSWORD || 'admin123';
  const adminName = process.env.ADMIN_NAME || 'Test Admin';

  console.log('🌱 Seeding database...');

  const [existingAdmin] = await db.select().from(user).where(eq(user.email, adminEmail)).limit(1);

  if (existingAdmin) {
    console.log(`⚠️  Admin user with email ${adminEmail} already exists. Skipping...`);
    return;
  }

  const passwordHash = await hash(adminPassword, 12);
  const now = new Date().toISOString();

  const [admin] = await db
    .insert(user)
    .values({
      id: crypto.randomUUID(),
      email: adminEmail,
      passwordHash,
      name: adminName,
      role: UserRole.ADMIN,
      createdAt: now,
      updatedAt: now,
    })
    .returning();

  if (admin) {
    console.log('✅ Test admin account created successfully!');
    console.log(`   Email: ${adminEmail}`);
    console.log(`   Password: ${adminPassword}`);
    console.log(`   Role: ${admin.role}`);
    console.log(`   ID: ${admin.id}`);
  }
}

main()
  .catch((e) => {
    console.error('❌ Error seeding database:', e);
    process.exitCode = 1;
  })
  .finally(async () => {
    await pool.end();
  });
