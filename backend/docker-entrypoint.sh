#!/bin/sh
set -e

echo "Running database migrations..."
bun run src/index.ts

echo "Starting server..."
exec bun run src/server.ts
