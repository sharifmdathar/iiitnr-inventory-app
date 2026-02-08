import type { PrismaClient as PrismaClientType } from '@prisma/client/extension';
import prismaDefault from '@prisma/client';
import { PrismaPg } from '@prisma/adapter-pg';
import pg from 'pg';
const { PrismaClient } = prismaDefault as unknown as {
  PrismaClient: new (options: { adapter: unknown }) => PrismaClientType;
};

const { Pool } = pg;

const isTest = process.env.NODE_ENV === 'test';
const databaseUrl = isTest
  ? process.env.TEST_DATABASE_URL || process.env.DATABASE_URL
  : process.env.DATABASE_URL;

if (!databaseUrl) {
  throw new Error(
    isTest ? 'TEST_DATABASE_URL or DATABASE_URL is required for tests' : 'DATABASE_URL is required',
  );
}

if (isTest && !process.env.TEST_DATABASE_URL && process.env.DATABASE_URL) {
  console.warn('⚠️  WARNING: Tests are using DATABASE_URL instead of TEST_DATABASE_URL!');
  console.warn(
    '⚠️  This may delete production data. Set TEST_DATABASE_URL to use a separate test database.',
  );
}

const pool = new Pool({
  connectionString: databaseUrl,
  keepAlive: true,
});

const adapter = new PrismaPg(pool);

export const prisma = new PrismaClient({ adapter });
export { pool };
