-- AlterTable
ALTER TABLE "Request" ADD COLUMN     "targetFacultyId" TEXT;

-- CreateIndex
CREATE INDEX "Request_targetFacultyId_idx" ON "Request"("targetFacultyId");

-- AddForeignKey
ALTER TABLE "Request" ADD CONSTRAINT "Request_targetFacultyId_fkey" FOREIGN KEY ("targetFacultyId") REFERENCES "User"("id") ON DELETE SET NULL ON UPDATE CASCADE;
