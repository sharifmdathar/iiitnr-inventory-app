-- Update RequestItem to reference Component instead of InventoryItem
ALTER TABLE "RequestItem" DROP CONSTRAINT IF EXISTS "RequestItem_itemId_fkey";
DROP INDEX IF EXISTS "RequestItem_itemId_idx";

ALTER TABLE "RequestItem" DROP COLUMN IF EXISTS "itemId";
ALTER TABLE "RequestItem" ADD COLUMN "componentId" TEXT NOT NULL;

CREATE INDEX "RequestItem_componentId_idx" ON "RequestItem"("componentId");

ALTER TABLE "RequestItem"
ADD CONSTRAINT "RequestItem_componentId_fkey"
FOREIGN KEY ("componentId") REFERENCES "Component"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- Drop InventoryItem table (requests now reference Component)
DROP TABLE IF EXISTS "InventoryItem";
