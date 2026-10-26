-- Codes are new identifiers; retain all historical names and primary/foreign keys.
ALTER TABLE dining_area ADD COLUMN code VARCHAR(40);
WITH base AS (
    SELECT id, CASE WHEN name = 'Tầng 1' THEN 'T01' WHEN name = 'Tầng 2' THEN 'T02'
        WHEN lower(name) = 'sân vườn' THEN 'SV' ELSE 'KV' || id END AS prefix FROM dining_area
), ranked AS (SELECT id, prefix, row_number() OVER (PARTITION BY prefix ORDER BY id) AS n FROM base)
UPDATE dining_area a SET code = CASE WHEN r.n = 1 THEN r.prefix ELSE 'KV' || a.id END FROM ranked r WHERE a.id = r.id;
ALTER TABLE dining_area ALTER COLUMN code SET NOT NULL;
ALTER TABLE dining_area ADD CONSTRAINT uq_dining_area_code UNIQUE (code);
ALTER TABLE dining_table ADD COLUMN code VARCHAR(64);
WITH numbered AS (SELECT id, row_number() OVER (PARTITION BY area_id ORDER BY id) AS n FROM dining_table)
UPDATE dining_table t SET code = a.code || '-B' || CASE WHEN n.n < 10 THEN '0' || n.n ELSE n.n::text END
FROM numbered n, dining_area a WHERE t.id = n.id AND a.id = t.area_id;
ALTER TABLE dining_table ALTER COLUMN code SET NOT NULL;
ALTER TABLE dining_table ADD CONSTRAINT uq_dining_table_code UNIQUE (code);
