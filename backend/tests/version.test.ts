import './test-setup.js';

import { describe, test, beforeAll, afterAll } from 'bun:test';
import assert from 'node:assert/strict';
import { buildApp } from '../src/app.js';
import fs from 'node:fs';

let app: Awaited<ReturnType<typeof buildApp>>;

beforeAll(async () => {
  app = await buildApp();
});

afterAll(async () => {
  await app.close();
});

describe('Version API', () => {
  test('GET /version returns correct version from package.json', async () => {
    const response = await app.inject({
      method: 'GET',
      url: '/version',
    });

    assert.equal(response.statusCode, 200);

    const pkg = JSON.parse(fs.readFileSync('./package.json', 'utf-8'));
    assert.equal(response.json().version, pkg.version);
  });
});
