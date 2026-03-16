import { drizzle } from 'drizzle-orm/node-postgres';
import { Pool } from 'pg';
import * as schema from './schema';
import {
  componentRelations,
  requestItemRelations,
  requestRelations,
  userRelations,
} from './relations';

const isTest = process.env.NODE_ENV === 'test';
const databaseUrl = isTest
  ? process.env.TEST_DATABASE_URL || process.env.DATABASE_URL
  : process.env.DATABASE_URL;

if (!databaseUrl) {
  throw new Error(
    isTest ? 'TEST_DATABASE_URL or DATABASE_URL is required' : 'DATABASE_URL is required',
  );
}

if (isTest && !process.env.TEST_DATABASE_URL && process.env.DATABASE_URL) {
  console.warn('⚠️  WARNING: Tests using DATABASE_URL instead of TEST_DATABASE_URL!');
}

export const pool = new Pool({ connectionString: databaseUrl });

export const db = drizzle(pool, {
  schema: {
    ...schema,
    requestRelations,
    userRelations,
    requestItemRelations,
    componentRelations,
  },
});
