UPDATE t_stock
SET available = 0
WHERE available IS NULL;

UPDATE t_stock
SET reserved = 0
WHERE reserved IS NULL;

ALTER TABLE t_stock
    MODIFY COLUMN available BIGINT NOT NULL DEFAULT 0 COMMENT '可用库存',
    MODIFY COLUMN reserved BIGINT NOT NULL DEFAULT 0 COMMENT '预占库存',
    ADD CONSTRAINT chk_stock_values
        CHECK (available >= 0 AND reserved >= 0 AND alert_threshold >= 0);

ALTER TABLE t_stock_journal
    ADD CONSTRAINT chk_stock_journal_values
        CHECK (type BETWEEN 1 AND 6 AND quantity <> 0);

ALTER TABLE t_stock_transfer
    ADD CONSTRAINT chk_stock_transfer_quantity
        CHECK (quantity > 0);

ALTER TABLE t_stock_count_task
    ADD CONSTRAINT chk_stock_count_values CHECK (
        expected_quantity >= 0
        AND (actual_quantity IS NULL OR actual_quantity >= 0)
        AND status BETWEEN 1 AND 3
    );
