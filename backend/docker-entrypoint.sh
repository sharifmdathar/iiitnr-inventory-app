#!/bin/sh
set -e

mkdir -p /app/uploads/images
chown -R bun:bun /app/uploads

echo "Running database migrations..."
bun run src/index.ts

echo "Starting server..."
exec bun run src/server.ts
