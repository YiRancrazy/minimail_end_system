ALTER TABLE t_order_outbox
    ADD CONSTRAINT chk_order_outbox_route
        CHECK (payload IS NULL OR (topic IS NOT NULL AND tag IS NOT NULL));
