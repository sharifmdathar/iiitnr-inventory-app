ALTER TYPE "public"."RequestStatus" ADD VALUE IF NOT EXISTS 'PARTIALLY_ISSUED' BEFORE 'RETURNED';--> statement-breakpoint
ALTER TYPE "public"."RequestStatus" ADD VALUE IF NOT EXISTS 'PARTIALLY_RETURNED' BEFORE 'RENEWED';--> statement-breakpoint
ALTER TYPE "public"."RequestStatus" ADD VALUE IF NOT EXISTS 'EXPIRED' BEFORE 'RENEWED';--> statement-breakpoint
ALTER TABLE "RequestItem" ADD COLUMN IF NOT EXISTS "fulfilledQuantity" integer DEFAULT 0 NOT NULL;--> statement-breakpoint
ALTER TABLE "RequestItem" ADD COLUMN IF NOT EXISTS "returnedQuantity" integer DEFAULT 0 NOT NULL;--> statement-breakpoint
ALTER TABLE "User" ADD COLUMN IF NOT EXISTS "batch" text;--> statement-breakpoint
ALTER TABLE "User" ADD COLUMN IF NOT EXISTS "branch" text;
