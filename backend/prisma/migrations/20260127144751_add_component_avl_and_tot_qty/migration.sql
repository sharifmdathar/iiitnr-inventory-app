/*
  Warnings:

  - You are about to drop the column `quantity` on the `Component` table. All the data in the column will be lost.

*/
-- AlterTable
ALTER TABLE "Component" DROP COLUMN "quantity",
ADD COLUMN     "availableQuantity" INTEGER NOT NULL DEFAULT 0,
ADD COLUMN     "totalQuantity" INTEGER NOT NULL DEFAULT 0;
