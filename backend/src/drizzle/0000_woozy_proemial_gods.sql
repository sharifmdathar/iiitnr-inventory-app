-- Idempotent migration: schema may already exist from Prisma
-- Uses exception handling to skip if objects already exist

DO $$ BEGIN
  CREATE TYPE "public"."ComponentCategory" AS ENUM('Sensors', 'Actuators', 'Microcontrollers', 'Microprocessors', 'Others');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;--> statement-breakpoint
DO $$ BEGIN
  CREATE TYPE "public"."Location" AS ENUM('IoT_Lab', 'Robo_Lab', 'VLSI_Lab');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;--> statement-breakpoint
DO $$ BEGIN
  CREATE TYPE "public"."RequestStatus" AS ENUM('PENDING', 'APPROVED', 'REJECTED', 'FULFILLED', 'RETURNED');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;--> statement-breakpoint
DO $$ BEGIN
  CREATE TYPE "public"."UserRole" AS ENUM('ADMIN', 'FACULTY', 'PENDING', 'STUDENT', 'TA');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;--> statement-breakpoint
CREATE TABLE IF NOT EXISTS "_prisma_migrations" (
	"id" varchar(36) PRIMARY KEY NOT NULL,
	"checksum" varchar(64) NOT NULL,
	"finished_at" timestamp with time zone,
	"migration_name" varchar(255) NOT NULL,
	"logs" text,
	"rolled_back_at" timestamp with time zone,
	"started_at" timestamp with time zone DEFAULT now() NOT NULL,
	"applied_steps_count" integer DEFAULT 0 NOT NULL
);
--> statement-breakpoint
CREATE TABLE IF NOT EXISTS "User" (
	"id" text PRIMARY KEY NOT NULL,
	"email" text NOT NULL,
	"name" text,
	"imageUrl" text,
	"passwordHash" text,
	"googleId" text,
	"role" "UserRole" DEFAULT 'PENDING' NOT NULL,
	"createdAt" timestamp(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
	"updatedAt" timestamp(3) NOT NULL
);
--> statement-breakpoint
CREATE TABLE IF NOT EXISTS "Request" (
	"id" text PRIMARY KEY NOT NULL,
	"userId" text NOT NULL,
	"targetFacultyId" text NOT NULL,
	"projectTitle" text NOT NULL,
	"status" "RequestStatus" DEFAULT 'PENDING' NOT NULL,
	"createdAt" timestamp(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
	"updatedAt" timestamp(3) NOT NULL
);
--> statement-breakpoint
CREATE TABLE IF NOT EXISTS "RequestItem" (
	"id" text PRIMARY KEY NOT NULL,
	"requestId" text NOT NULL,
	"componentId" text NOT NULL,
	"quantity" integer NOT NULL,
	"createdAt" timestamp(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
	"updatedAt" timestamp(3) NOT NULL
);
--> statement-breakpoint
CREATE TABLE IF NOT EXISTS "Component" (
	"id" text PRIMARY KEY NOT NULL,
	"name" text NOT NULL,
	"description" text,
	"imageUrl" text,
	"totalQuantity" integer DEFAULT 0 NOT NULL,
	"availableQuantity" integer DEFAULT 0 NOT NULL,
	"category" "ComponentCategory",
	"location" "Location",
	"createdAt" timestamp(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
	"updatedAt" timestamp(3) NOT NULL
);
--> statement-breakpoint
DO $$ BEGIN
  ALTER TABLE "Request" ADD CONSTRAINT "Request_targetFacultyId_fkey" FOREIGN KEY ("targetFacultyId") REFERENCES "public"."User"("id") ON DELETE restrict ON UPDATE cascade;
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;--> statement-breakpoint
DO $$ BEGIN
  ALTER TABLE "Request" ADD CONSTRAINT "Request_userId_fkey" FOREIGN KEY ("userId") REFERENCES "public"."User"("id") ON DELETE restrict ON UPDATE cascade;
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;--> statement-breakpoint
DO $$ BEGIN
  ALTER TABLE "RequestItem" ADD CONSTRAINT "RequestItem_componentId_fkey" FOREIGN KEY ("componentId") REFERENCES "public"."Component"("id") ON DELETE restrict ON UPDATE cascade;
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;--> statement-breakpoint
DO $$ BEGIN
  ALTER TABLE "RequestItem" ADD CONSTRAINT "RequestItem_requestId_fkey" FOREIGN KEY ("requestId") REFERENCES "public"."Request"("id") ON DELETE restrict ON UPDATE cascade;
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;--> statement-breakpoint
CREATE UNIQUE INDEX IF NOT EXISTS "User_email_key" ON "User" USING btree ("email" text_ops);--> statement-breakpoint
CREATE UNIQUE INDEX IF NOT EXISTS "User_googleId_key" ON "User" USING btree ("googleId" text_ops);--> statement-breakpoint
CREATE INDEX IF NOT EXISTS "Request_targetFacultyId_idx" ON "Request" USING btree ("targetFacultyId" text_ops);--> statement-breakpoint
CREATE INDEX IF NOT EXISTS "Request_userId_idx" ON "Request" USING btree ("userId" text_ops);--> statement-breakpoint
CREATE INDEX IF NOT EXISTS "RequestItem_componentId_idx" ON "RequestItem" USING btree ("componentId" text_ops);--> statement-breakpoint
CREATE INDEX IF NOT EXISTS "RequestItem_requestId_idx" ON "RequestItem" USING btree ("requestId" text_ops);
