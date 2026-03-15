# IIITNR Inventory App

[![Release](https://img.shields.io/github/v/release/sharifmdathar/iiitnr-inventory-app?label=Release&style=for-the-badge)](https://github.com/sharifmdathar/iiitnr-inventory-app/releases)
[![CodeFactor](https://img.shields.io/codefactor/grade/github/sharifmdathar/iiitnr-inventory-app?label=CodeFactor&style=for-the-badge)](https://www.codefactor.io/repository/github/sharifmdathar/iiitnr-inventory-app)

A monorepo inventory management system with a **Kotlin Multiplatform app** (Android + iOS + Desktop) and a Fastify + Prisma backend.

## Features

- **User Authentication**: JWT-based authentication with role-based access control
- **Component Management**: CRUD operations for inventory components (Admin/TA only)
- **Request System**: Users can create requests for components, admins/TAs can view and manage requests
- **Role-Based Access**: User roles (ADMIN, FACULTY, STUDENT, TA, PENDING) with different permissions; new registrations start as PENDING until promoted
- **Database Seeding**: Automated admin user creation for initial setup
- **Comprehensive Testing**: Full test suite for authentication, components, and requests
- **HTTP Caching & Local Cache**:
  - Backend supports conditional `GET` via `If-Modified-Since` / `Last-Modified` and returns `304 Not Modified` when the client cache is fresh.
  - The KMP app uses **SQLDelight** to cache the components list locally and drives the UI from the cache, so the list remains visible across navigation and 304 responses.

## Prerequisites

- [Bun](https://bun.sh)
- PostgreSQL (local or hosted, e.g. Supabase/Neon)
- Docker or Podman with Compose (optional: for local PostgreSQL and/or running the backend in a container)

## Installation

### Backend dependencies

```bash
cd backend
bun install
```

### KMP app (Android + Desktop)

The UI lives in the `app/` multiplatform module:

- **Android**:
  - Open the repo in Android Studio.
  - Use a recent JDK (21+) and Android Gradle Plugin (already configured).
  - Run the `android` run configuration or `./gradlew :android:installDebug` from `app/`.

- **Desktop**:
  - Requires JDK 21+.
  - From `app/`:
    ```bash
    ./gradlew :desktop:run
    ```
  - This launches the Compose Desktop app (AppImage/MSI/EXE packaging is configured for releases).

## Database Setup

### Using Docker Compose

Compose uses **profiles** so the main and test databases don’t run at the same time (same port). From `backend/`:

| Command | Effect |
|--------|--------|
| `podman compose -f compose.db.yaml --profile main up -d` | Start **main** Postgres (port 5432, DB `iiitnr_inventory`, persisted) |
| `podman compose -f compose.db.yaml --profile test up -d` | Start **test** Postgres (port 5432, DB `iiitnr_inventory_test`, ephemeral) |

Main DB (development):

```bash
cd backend
podman compose -f compose.db.yaml --profile main up -d
```

- Database: `iiitnr_inventory`
- User: `postgres`
- Password: `postgres`

Test DB (used by `just test`; ephemeral, no volume):

```bash
cd backend
podman compose -f compose.db.yaml --profile test up -d
```

### Cloud or manual PostgreSQL

Use a hosted DB (e.g. Supabase, Neon) or install Postgres yourself. Set `DATABASE_URL` in `backend/.env`; no `POSTGRES_*` vars are required.

## Backend Setup

### Environment Configuration

Copy the example environment file:

```bash
cp backend/config/env.example backend/.env
```

Edit `backend/.env` and set your Environment Variables

**Important**: Change `JWT_SECRET` to a secure random string. The application will not start if it's set to "change-me".

### Database Migrations

**Development** (creates migration files, applies them, and generates the client):

```bash
cd backend
bun run prisma:generate
bun run prisma:migrate
```

**Production / container** (apply existing migrations only; run once before starting the backend):

```bash
cd backend
bunx prisma migrate deploy
```

### Seed Database

Create an admin user:

```bash
cd backend
bun run seed
```

This creates an admin account with the credentials specified in your `.env` file (defaults shown above).

### Start Development Server

```bash
cd backend
bun run dev
```

The server will start on `http://localhost:4000` (or the port specified in `PORT`).

### Running the backend in a container

Use the pre-built image or build from `backend/Containerfile` (or `Dockerfile`). From `backend/`:

1. **Migrations**: Run once (e.g. for Supabase/Neon or a remote DB):
   ```bash
   bunx prisma migrate deploy
   ```
2. **Compose** (uses `backend/compose.yaml`: image, `env_file: .env`, port 4000):
   ```bash
   podman compose up -d
   ```
   Ensure `backend/.env` exists and has `DATABASE_URL` (and `JWT_SECRET`, etc.). The backend listens on port 4000.

**Build the image locally** (from `backend/`):

```bash
podman build -t iiitnr-inventory-backend -f Containerfile .
# or: docker build -t iiitnr-inventory-backend .
```

Then run with `podman run --rm -p 4000:4000 --env-file .env iiitnr-inventory-backend`, or point `compose.yaml` at your image.

## API Endpoints

### Health Check

- `GET /health` - Health check endpoint
- `GET /` - Hello World endpoint

### Authentication

All authentication endpoints are public (no auth required).

- `POST /auth/register` - Register a new user
  - Body: `{ email, password, name? }`
  - New users receive role `PENDING` until an admin promotes them
  - Returns: `{ user, token }`

- `POST /auth/login` - Login with email and password
  - Body: `{ email, password }`
  - Returns: `{ user, token }`

- `GET /auth/me` - Get current user info (requires authentication)
  - Headers: `Authorization: Bearer <token>`
  - Returns: `{ user }`

### Components

All component endpoints require authentication. Create/Update/Delete require Admin or TA role.

- `GET /components` - List all components (authenticated users)
- `GET /components/:id` - Get component by ID (authenticated users)
- `POST /components` - Create a new component (Admin/TA only)
  - Body: `{ name, description?, quantity?, category?, location? }`
- `PUT /components/:id` - Update component (Admin/TA only)
  - Body: `{ name?, description?, quantity?, category?, location? }`
- `DELETE /components/:id` - Delete component (Admin/TA only)

### Requests

All request endpoints require authentication. Users can only see their own requests unless they're Admin/TA.

- `POST /requests` - Create a new request (authenticated users)
  - Body: `{ items: [{ componentId, quantity }] }`
  - Returns: `{ request }` with items and component details

- `GET /requests` - List requests (authenticated users)
  - Query params: `?status=PENDING|APPROVED|REJECTED|FULFILLED` (optional)
  - Query params: `?userId=<uuid>` (Admin/TA only, optional)
  - Students/Faculty: Only see their own requests
  - Admin/TA: See all requests, optionally filtered by userId
  - Returns: `{ requests }` with items, components, and user details

## User Roles

- **ADMIN**: Full access to all endpoints, can manage components and view all requests
- **TA** (Teaching Assistant): Can manage components and view all requests
- **FACULTY**: Can view components and create/view their own requests
- **STUDENT**: Can view components and create/view their own requests
- **PENDING**: New registrations start here; protected routes return 403 until an admin assigns a role (e.g. via `bun run create:user`)

## Testing

**Recommended** – from repo root, use the Justfile to start the ephemeral test DB, run migrations, then tests:

```bash
just test
```

This starts the test DB (`compose.db.yaml --profile test`), runs `bun test` (using `TEST_DATABASE_URL` or `DATABASE_URL` from `backend/.env`), then stops the test DB.

To run tests only (test DB must already be running, either locally or on remote):

```bash
cd backend
bun test
```

The test suite includes:
- Authentication tests (`tests/auth.test.ts`)
- Component management tests (`tests/components.test.ts`)
- Request system tests (`tests/requests.test.ts`)

**Important**: `TEST_DATABASE_URL` (or `DATABASE_URL` for derivation) must be set; the test setup exits with an error if not. Use a dedicated test database (e.g. name containing `_test`) and keep it different from production.

## Development Scripts

### Backend (Bun)

From `backend/`:

- `bun run dev` - Start development server with hot reload
- `bun run build` - Build for production
- `bun run start` - Start production server
- `bun run lint` - Run ESLint
- `bun run lint:fix` - Fix ESLint errors
- `bun run typecheck` - Type check TypeScript
- `bun run format` - Format backend code with Prettier
- `bun run prisma:generate` - Generate Prisma client
- `bun run prisma:migrate` - Run database migrations
- `bun run seed` - Seed database with admin user
- `bun run migrate:backup` - Backup app data, reset public schema, run migrations, then restore data (works on Supabase/Neon; requires `pg_dump` and `psql`). Optional: `bun run migrate:backup -- --restore-from=backups/data_YYYY-MM-DD.sql` to restore from a specific backup file.
- `bun run create:user` - Create a user (e.g. promote PENDING to a role). Usage: `bun run create:user -- --email ... --password ... --role PENDING|STUDENT|FACULTY|TA|ADMIN [--name "Name"]`

### Root Justfile

At the repo root, `Justfile` streamlines common tasks:

- `just` / `just dev` — Start backend dev server
- `just install` — Install backend dependencies
- `just image` — Build backend image (Containerfile) and run with `--env-file .env -p 4000:4000`
- `just up` — Start backend container (`podman compose up -d`)
- `just down` — Stop backend container
- `just restart` — `compose down` then `up -d`
- `just logs` — Follow compose logs
- `just test` — Start test DB (`compose.db.yaml --profile test`), run `bun test`, then stop DB
- `just desk` — Run KMP desktop app
- `just lint` — Lint backend + `ktlintCheck` (app)
- `just lint-fix` — Lint fix + `ktlintFormat`
- `just typecheck` — Backend typecheck
- `just fmt` — Format backend + `ktlintFormat`
- `just detekt` — Detekt on the app

## Mobile App

The mobile app is an Android application built with Kotlin. See the `app/` directory for Android-specific setup and build instructions.

## Deployment

### Render.com

To deploy the backend to Render:

1. Create a new Web Service pointing at this repository
2. Root directory: `backend`
3. Build command: `bun install --frozen-lockfile && bun run build`
4. Start command: `bun run start`
5. Add environment variables:
   - `DATABASE_URL` (required)
   - `JWT_SECRET` (required, must be changed from "change-me")
   - `PORT` (optional, defaults to 4000)

The health check endpoint is available at `GET /health` for Render's health checks.

## Project Structure

```
.
├── backend/              # Fastify backend API
│   ├── config/           # Config (e.g. env.example)
│   ├── prisma/           # Prisma schema and migrations
│   ├── src/              # Source code
│   │   ├── app.ts        # Fastify app and routes
│   │   ├── server.ts     # Entry point (DB + migration checks at startup)
│   │   └── lib/          # Prisma client
│   ├── tests/            # Test suite
│   ├── compose.yaml      # Backend service (image, env_file, port 4000)
│   ├── compose.db.yaml   # Local Postgres only (profiles: main, test)
│   ├── Containerfile     # Multi-stage image build (Bun → runner)
│   └── prisma.config.ts  # Prisma 7 config (DB URL, schema path)
├── app/                  # KMP app (Android, Desktop)
└── Justfile              # Tasks: dev, test, image, up, down, lint, etc.
```

## Database Schema

The application uses the following main models:

- **User**: Authentication and user information with roles
- **Component**: Inventory components with quantity, category, and location
- **Request**: User requests for components with status tracking
- **RequestItem**: Individual items within a request linking components to requests

See `backend/prisma/schema.prisma` for the complete schema definition.

## License

Private project for IIITNR.
