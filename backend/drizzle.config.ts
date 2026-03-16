import 'dotenv/config';
import { defineConfig } from 'drizzle-kit';

const nodeEnv = process.env.NODE_ENV;
const isTest = nodeEnv === 'test';

const databaseUrl = isTest
  ? process.env.TEST_DATABASE_URL || process.env.DATABASE_URL
  : process.env.DATABASE_URL;

if (!databaseUrl) {
  throw new Error(
    isTest
      ? 'TEST_DATABASE_URL or DATABASE_URL is required for Drizzle.'
      : 'DATABASE_URL is required for Drizzle.',
  );
}

export default defineConfig({
  dialect: 'postgresql',
  out: './src/drizzle',
  schema: './src/drizzle/schema.ts',
  dbCredentials: {
    url: databaseUrl,
  },
  verbose: true,
  strict: true,
});
