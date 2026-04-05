ALTER TABLE "Request" ADD COLUMN "returnDueAt" timestamp(3);--> statement-breakpoint
ALTER TABLE "Request" ADD COLUMN "returnedAt" timestamp(3);--> statement-breakpoint
ALTER TABLE "Request" ADD COLUMN "fulfilledAt" timestamp(3);--> statement-breakpoint
ALTER TABLE "Request" ADD COLUMN "receivedByUserId" text;