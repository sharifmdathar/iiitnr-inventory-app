# IIITNR Inventory App

Monorepo with a React Native mobile app and a Fastify + Prisma backend.

## Prereqs

- Node.js 20+
- pnpm 10+
- PostgreSQL (local or hosted)

## Install

```bash
pnpm install
```

## Mobile (React Native)

```bash
pnpm --filter mobile start
```

Lint / typecheck:

```bash
pnpm --filter mobile lint
pnpm --filter mobile typecheck
```

## Backend (Fastify)

Copy env vars:

```bash
cp backend/config/env.example backend/.env
```

Generate Prisma client and run migrations:

```bash
pnpm --filter @iiitnr/backend prisma:generate
pnpm --filter @iiitnr/backend prisma:migrate
```

Start the server:

```bash
pnpm --filter @iiitnr/backend dev
```

### Render deploy (Hello World)

- Create a new Web Service pointing at this repo.
- Root directory: `backend`
- Build command: `pnpm install --frozen-lockfile && pnpm build`
- Start command: `pnpm start`
- Add env var `DATABASE_URL` (and optionally `PORT`)

Health check route: `GET /health`
