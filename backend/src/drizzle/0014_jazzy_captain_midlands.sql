CREATE TYPE "public"."RequestEmailKind" AS ENUM('RENEWAL_CHASE', 'EXPIRY_NOTICE', 'LATE_FEE');--> statement-breakpoint
CREATE TABLE "RequestEmail" (
	"id" text PRIMARY KEY NOT NULL,
	"requestId" text NOT NULL,
	"recipientId" text NOT NULL,
	"kind" "RequestEmailKind" NOT NULL,
	"variant" text NOT NULL,
	"subject" text,
	"metadata" jsonb,
	"sentAt" timestamp(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);
--> statement-breakpoint
ALTER TABLE "Request" ADD COLUMN "renewalRequestedAt" timestamp(3);--> statement-breakpoint
ALTER TABLE "Request" ADD COLUMN "lateFeeAccrued" integer DEFAULT 0 NOT NULL;--> statement-breakpoint
ALTER TABLE "RequestEmail" ADD CONSTRAINT "RequestEmail_requestId_fkey" FOREIGN KEY ("requestId") REFERENCES "public"."Request"("id") ON DELETE cascade ON UPDATE cascade;--> statement-breakpoint
ALTER TABLE "RequestEmail" ADD CONSTRAINT "RequestEmail_recipientId_fkey" FOREIGN KEY ("recipientId") REFERENCES "public"."User"("id") ON DELETE cascade ON UPDATE cascade;--> statement-breakpoint
CREATE UNIQUE INDEX "RequestEmail_dedupe_key" ON "RequestEmail" USING btree ("requestId" text_ops,"recipientId" text_ops,"kind","variant" text_ops);--> statement-breakpoint
CREATE INDEX "RequestEmail_requestId_idx" ON "RequestEmail" USING btree ("requestId" text_ops);--> statement-breakpoint
CREATE INDEX "RequestEmail_sentAt_idx" ON "RequestEmail" USING btree ("sentAt");