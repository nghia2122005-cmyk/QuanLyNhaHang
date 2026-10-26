ALTER TABLE order_item ADD COLUMN unavailable_acknowledged_at TIMESTAMPTZ;
ALTER TABLE order_item ADD COLUMN unavailable_acknowledged_by_id BIGINT REFERENCES account(id) ON DELETE RESTRICT;
ALTER TABLE order_item ADD CONSTRAINT ck_unavailable_acknowledgment
    CHECK ((unavailable_acknowledged_at IS NULL AND unavailable_acknowledged_by_id IS NULL)
        OR (status = 'UNAVAILABLE' AND unavailable_acknowledged_at IS NOT NULL AND unavailable_acknowledged_by_id IS NOT NULL));
