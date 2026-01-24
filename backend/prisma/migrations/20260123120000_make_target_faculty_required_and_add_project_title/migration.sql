-- Add optional Project Title
ALTER TABLE "Request" ADD COLUMN "projectTitle" TEXT;

-- Backfill NULL targetFacultyId with first available faculty (if any).
-- If no faculty exists and rows have NULL, the following ALTER will fail:
-- fix or delete those rows before migrating.
UPDATE "Request"
SET "targetFacultyId" = (SELECT id FROM "User" WHERE "role" = 'FACULTY' ORDER BY "createdAt" ASC LIMIT 1)
WHERE "targetFacultyId" IS NULL;

-- Update FK: ON DELETE SET NULL is invalid now that targetFacultyId will be NOT NULL
ALTER TABLE "Request" DROP CONSTRAINT "Request_targetFacultyId_fkey";
ALTER TABLE "Request" ADD CONSTRAINT "Request_targetFacultyId_fkey" FOREIGN KEY ("targetFacultyId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- Make targetFacultyId required
ALTER TABLE "Request" ALTER COLUMN "targetFacultyId" SET NOT NULL;
