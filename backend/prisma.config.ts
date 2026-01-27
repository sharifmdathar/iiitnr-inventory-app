import 'dotenv/config';
import { defineConfig } from 'prisma/config';

const nodeEnv = process.env.NODE_ENV;
const isTest = nodeEnv === 'test';

const databaseUrl = isTest
  ? process.env.TEST_DATABASE_URL || process.env.DATABASE_URL
  : process.env.DATABASE_URL;

if (!databaseUrl) {
  throw new Error(
    isTest
      ? 'TEST_DATABASE_URL or DATABASE_URL is required for Prisma.'
      : 'DATABASE_URL is required for Prisma.',
  );
}

export default defineConfig({
  schema: 'prisma/schema.prisma',
  datasource: {
    url: databaseUrl,
  },
});
