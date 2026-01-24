-- Make projectTitle required (assumes all rows have been backfilled with a non-null title)
ALTER TABLE "Request" ALTER COLUMN "projectTitle" SET NOT NULL;
