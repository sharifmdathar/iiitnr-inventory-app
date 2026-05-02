ALTER TYPE "public"."RequestStatus" ADD VALUE 'RENEWED';--> statement-breakpoint
ALTER TYPE "public"."RequestStatus" ADD VALUE 'REQUESTED_RENEW';--> statement-breakpoint
ALTER TABLE "Request" ADD COLUMN "lastRenewDate" timestamp(3);--> statement-breakpoint
ALTER TABLE "Request" ADD COLUMN "lastRenewReason" text;