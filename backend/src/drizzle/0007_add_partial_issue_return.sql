ALTER TABLE "RequestItem" ADD COLUMN IF NOT EXISTS "fulfilledQuantity" integer DEFAULT 0 NOT NULL;--> statement-breakpoint
ALTER TYPE "public"."RequestStatus" ADD VALUE IF NOT EXISTS 'PARTIALLY_ISSUED';--> statement-breakpoint
ALTER TYPE "public"."RequestStatus" ADD VALUE IF NOT EXISTS 'PARTIALLY_RETURNED';
