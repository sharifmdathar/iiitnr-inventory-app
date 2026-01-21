# IIITNR Inventory App

A monorepo inventory management system with an Android mobile app and a Fastify + Prisma backend.

## Features

- **User Authentication**: JWT-based authentication with role-based access control
- **Component Management**: CRUD operations for inventory components (Admin/TA only)
- **Request System**: Users can create requests for components, admins/TAs can view and manage requests
- **Role-Based Access**: Four user roles (ADMIN, FACULTY, STUDENT, TA) with different permissions
- **Database Seeding**: Automated admin user creation for initial setup
- **Comprehensive Testing**: Full test suite for authentication, components, and requests

## Prerequisites

- Node.js 20+
- pnpm 10+
- PostgreSQL (local or hosted)
- Docker & Docker Compose (optional, for local PostgreSQL)

## Installation

```bash
pnpm install
```

## Database Setup

### Using Docker Compose (Recommended)

Start PostgreSQL with Docker Compose:

```bash
docker compose up -d
```

This will start a PostgreSQL container on port 5432 with:
- Database: `iiitnr_inventory`
- User: `postgres`
- Password: `postgres`

### Manual Setup

Alternatively, set up PostgreSQL manually and configure the connection string in your `.env` file.

## Backend Setup

### Environment Configuration

Copy the example environment file:

```bash
cp backend/config/env.example backend/.env
```

Edit `backend/.env` and configure:

```env
DATABASE_URL="postgresql://postgres:postgres@localhost:5432/iiitnr_inventory"
TEST_DATABASE_URL="postgresql://postgres:postgres@localhost:5432/iiitnr_inventory_test"
PORT=4000
JWT_SECRET="your-secret-key-here"  # Must be changed from "change-me"

# Optional: Seed script configuration
ADMIN_EMAIL="admin@test.com"
ADMIN_PASSWORD="admin123"
ADMIN_NAME="Test Admin"
```

**Important**: Change `JWT_SECRET` to a secure random string. The application will not start if it's set to "change-me".

### Database Migrations

Generate Prisma client and run migrations:

```bash
pnpm --filter @iiitnr/backend prisma:generate
pnpm --filter @iiitnr/backend prisma:migrate
```

### Seed Database

Create an admin user:

```bash
pnpm --filter @iiitnr/backend seed
```

This creates an admin account with the credentials specified in your `.env` file (defaults shown above).

### Start Development Server

```bash
pnpm --filter @iiitnr/backend dev
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

Run the test suite:

```bash
pnpm --filter @iiitnr/backend test
```

The test suite includes:
- Authentication tests (`tests/auth.test.ts`)
- Component management tests (`tests/components.test.ts`)
- Request system tests (`tests/requests.test.ts`)

**Important**: Tests use a separate test database (`TEST_DATABASE_URL`). Make sure it's configured and different from your production database.

## Development Scripts

### Backend

- `pnpm --filter @iiitnr/backend dev` - Start development server with hot reload
- `pnpm --filter @iiitnr/backend build` - Build for production
- `pnpm --filter @iiitnr/backend start` - Start production server
- `pnpm --filter @iiitnr/backend lint` - Run ESLint
- `pnpm --filter @iiitnr/backend lint:fix` - Fix ESLint errors
- `pnpm --filter @iiitnr/backend typecheck` - Type check TypeScript
- `pnpm --filter @iiitnr/backend format` - Format code with Prettier
- `pnpm --filter @iiitnr/backend test` - Run tests
- `pnpm --filter @iiitnr/backend prisma:generate` - Generate Prisma client
- `pnpm --filter @iiitnr/backend prisma:migrate` - Run database migrations
- `pnpm --filter @iiitnr/backend seed` - Seed database with admin user

### Root

- `pnpm lint` - Lint all packages
- `pnpm lint:fix` - Fix linting errors in all packages
- `pnpm typecheck` - Type check all packages
- `pnpm format` - Format all packages

## Mobile App

The mobile app is an Android application built with Kotlin. See the `app/` directory for Android-specific setup and build instructions.

## Deployment

### Render.com

To deploy the backend to Render:

1. Create a new Web Service pointing at this repository
2. Root directory: `backend`
3. Build command: `pnpm install --frozen-lockfile && pnpm build`
4. Start command: `pnpm start`
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
├── app/               # Android mobile app
├── compose.yaml       # Docker Compose configuration
└── package.json       # Root package.json for monorepo
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
