CREATE TYPE "public"."AuditActionType" AS ENUM('CREATE', 'UPDATE', 'DELETE', 'LOGIN', 'LOGOUT', 'REQUEST_STATUS_CHANGE', 'INVENTORY_ADJUST');--> statement-breakpoint
CREATE TABLE "AuditLog" (
	"id" text PRIMARY KEY NOT NULL,
	"userId" text,
	"action" "AuditActionType" NOT NULL,
	"entityType" text,
	"entityId" text,
	"oldValues" text,
	"newValues" text,
	"ipAddress" varchar(45),
	"userAgent" text,
	"metadata" text,
	"createdAt" timestamp(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);
--> statement-breakpoint
ALTER TABLE "AuditLog" ADD CONSTRAINT "AuditLog_userId_fkey" FOREIGN KEY ("userId") REFERENCES "public"."User"("id") ON DELETE set null ON UPDATE cascade;--> statement-breakpoint
CREATE INDEX "AuditLog_userId_idx" ON "AuditLog" USING btree ("userId" text_ops);--> statement-breakpoint
CREATE INDEX "AuditLog_entityType_idx" ON "AuditLog" USING btree ("entityType" text_ops);--> statement-breakpoint
CREATE INDEX "AuditLog_entityId_idx" ON "AuditLog" USING btree ("entityId" text_ops);--> statement-breakpoint
CREATE INDEX "AuditLog_createdAt_idx" ON "AuditLog" USING btree ("createdAt" timestamp_ops);