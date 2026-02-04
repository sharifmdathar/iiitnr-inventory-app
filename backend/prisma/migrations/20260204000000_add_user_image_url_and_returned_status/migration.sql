-- AlterTable
ALTER TABLE "User" ADD COLUMN "imageUrl" TEXT;

-- AlterEnum
ALTER TYPE "RequestStatus" ADD VALUE 'RETURNED';