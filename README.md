# IIITNR Inventory App

[![Release](https://img.shields.io/github/v/release/sharifmdathar/iiitnr-inventory-app?label=Release&style=for-the-badge)](https://github.com/sharifmdathar/iiitnr-inventory-app/releases)
[![CodeFactor](https://img.shields.io/codefactor/grade/github/sharifmdathar/iiitnr-inventory-app?label=CodeFactor&style=for-the-badge)](https://www.codefactor.io/repository/github/sharifmdathar/iiitnr-inventory-app)

A monorepo inventory management system with an Android mobile app and a Fastify + Prisma backend.

## Features

- **User Authentication**: JWT-based authentication with role-based access control
- **Component Management**: CRUD operations for inventory components (Admin/TA only)
- **Request System**: Users can create requests for components, admins/TAs can view and manage requests
- **Role-Based Access**: Four user roles (ADMIN, FACULTY, STUDENT, TA) with different permissions
- **Database Seeding**: Automated admin user creation for initial setup
- **Comprehensive Testing**: Full test suite for authentication, components, and requests

## Prerequisites

- [Bun](https://bun.sh)
- PostgreSQL (local or hosted)
- Docker & Docker Compose (optional, for local PostgreSQL)

## Installation

### Backend dependencies

```bash
cd backend
bun install
```

### Android app

Follow the usual Android/Kotlin setup for the `app/` module (Android Studio with a recent Gradle and JDK)

## Database Setup

### Using Docker Compose

Compose uses **profiles** so the main and test databases don’t run at the same time (same port). From `backend/`:

| Command | Effect |
|--------|--------|
| `docker compose --profile prod up -d` | Start **main** Postgres (port 5432, DB `iiitnr_inventory`, data persisted) |
| `docker compose --profile test up -d` | Start **test** Postgres (port 5432, DB `iiitnr_inventory_test`, ephemeral) |

Main DB (development):

```bash
cd backend
docker compose --profile main up -d
```

- Database: `iiitnr_inventory`
- User: `postgres`
- Password: `postgres`

Test DB (used by `just test`; ephemeral, no volume):

```bash
cd backend
docker compose --profile test up -d
```

### Manual Setup

Alternatively, set up PostgreSQL manually and configure the connection string in your `.env` file.

## Backend Setup

### Environment Configuration

Copy the example environment file:

```bash
cp backend/config/env.example backend/.env
```

Edit `backend/.env` and set your Environment Variables

**Important**: Change `JWT_SECRET` to a secure random string. The application will not start if it's set to "change-me".

### Database Migrations

Generate Prisma client and run migrations:

```bash
cd backend
bun run prisma:generate
bun run prisma:migrate
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

## API Endpoints

### Health Check

- `GET /health` - Health check endpoint
- `GET /` - Hello World endpoint

### Authentication

All authentication endpoints are public (no auth required).

- `POST /auth/register` - Register a new user
  - Body: `{ email, password, name?, role? }`
  - Roles: `STUDENT` (default), `FACULTY`, `TA` (ADMIN cannot be registered via API)
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

## Testing

**Recommended** – from repo root, use the Justfile to start the ephemeral test DB, run migrations, then tests:

```bash
just test
```

This runs `docker compose --profile test up -d`, migrates using `TEST_DATABASE_URL` from `backend/.env`, runs `bun test`, then brings the test DB down.

To run tests only (test DB must already be running, either locally or on remote):

```bash
cd backend
bun test
```

The test suite includes:
- Authentication tests (`tests/auth.test.ts`)
- Component management tests (`tests/components.test.ts`)
- Request system tests (`tests/requests.test.ts`)

**Important**: Tests use a separate test database (`TEST_DATABASE_URL`). Make sure it's configured and different from your production database.

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

### Root Justfile

At the repo root there is a `Justfile` to streamline common tasks:

- `just` or `just dev` - Start the backend dev server
- `just install` - Install backend dependencies
- `just test` - Start test DB (`--profile test`), run migrations and `bun test`, then stop test DB
- `just lint` - Lint backend and run `ktlintCheck` for the Android app
- `just lint-fix` - Fix backend lint issues and run `ktlintFormat` for the Android app
- `just typecheck` - Type-check the backend
- `just fmt` - Format backend code and run `ktlintFormat` for the Android app
- `just detekt` - Run Detekt on the Android app

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
├── backend/           # Fastify backend API
│   ├── config/        # Configuration files
│   ├── prisma/        # Prisma schema and migrations
│   ├── src/           # Source code
│   │   ├── app.ts     # Fastify app setup and routes
│   │   ├── server.ts  # Server entry point
│   │   └── lib/       # Library code (Prisma client)
│   ├── tests/         # Test suite
│   └── dist/          # Compiled output
├── app/               # KMP app (frontend)
├── backend/
│   └── compose.yaml   # Docker Compose file for local database (optional)
└── Justfile           # Root task runner for backend + app
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
