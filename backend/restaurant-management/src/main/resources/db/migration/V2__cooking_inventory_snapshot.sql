-- Reservations are immutable requirement snapshots on existing order items, not ledger entries.
-- Only COOKING snapshots count as held stock. READY consumes; UNAVAILABLE releases.
-- NULL marks legacy/unstarted rows; [] explicitly captures a recipe with no ingredients.
ALTER TABLE order_item ADD COLUMN ingredient_requirements JSONB;
ALTER TABLE order_item ADD CONSTRAINT ck_order_item_requirements_array
    CHECK (ingredient_requirements IS NULL OR jsonb_typeof(ingredient_requirements) = 'array');
CREATE INDEX idx_order_item_cooking ON order_item(id) WHERE status = 'COOKING';
