ALTER TYPE "public"."RequestStatus" ADD VALUE IF NOT EXISTS 'RENEWED';--> statement-breakpoint
ALTER TYPE "public"."RequestStatus" ADD VALUE IF NOT EXISTS 'REQUESTED_RENEW';--> statement-breakpoint
ALTER TABLE "Request" ADD COLUMN IF NOT EXISTS "lastRenewDate" timestamp(3);--> statement-breakpoint
ALTER TABLE "Request" ADD COLUMN IF NOT EXISTS "lastRenewReason" text;
