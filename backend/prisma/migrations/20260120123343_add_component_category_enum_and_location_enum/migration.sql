/*
  Warnings:

  - The `category` column on the `Component` table would be dropped and recreated. This will lead to data loss if there is data in the column.
  - The `location` column on the `Component` table would be dropped and recreated. This will lead to data loss if there is data in the column.

*/
-- CreateEnum
CREATE TYPE "ComponentCategory" AS ENUM ('Sensors', 'Actuators', 'Microcontrollers', 'Microprocessors', 'Others');

-- CreateEnum
CREATE TYPE "Location" AS ENUM ('IoT_Lab', 'Robo_Lab', 'VLSI_Lab');

-- AlterTable
ALTER TABLE "Component" DROP COLUMN "category",
ADD COLUMN     "category" "ComponentCategory",
DROP COLUMN "location",
ADD COLUMN     "location" "Location";
