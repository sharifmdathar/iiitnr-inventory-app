CREATE TABLE "Notification" (
	"id" text PRIMARY KEY NOT NULL,
	"recipientId" text NOT NULL,
	"type" text NOT NULL,
	"title" text NOT NULL,
	"body" text,
	"requestId" text,
	"read" boolean DEFAULT false NOT NULL,
	"createdAt" timestamp(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);
--> statement-breakpoint
ALTER TABLE "Notification" ADD CONSTRAINT "Notification_recipientId_fkey" FOREIGN KEY ("recipientId") REFERENCES "public"."User"("id") ON DELETE cascade ON UPDATE cascade;--> statement-breakpoint
ALTER TABLE "Notification" ADD CONSTRAINT "Notification_requestId_fkey" FOREIGN KEY ("requestId") REFERENCES "public"."Request"("id") ON DELETE set null ON UPDATE cascade;--> statement-breakpoint
CREATE INDEX "Notification_recipientId_createdAt_idx" ON "Notification" USING btree ("recipientId","createdAt");--> statement-breakpoint
CREATE INDEX "Notification_recipientId_read_idx" ON "Notification" USING btree ("recipientId","read");