SET @journal_sku_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 't_stock_journal'
      AND index_name = 'idx_journal_sku_id'
);
SET @drop_journal_sku_index = IF(
    @journal_sku_index_exists > 0,
    'ALTER TABLE t_stock_journal DROP INDEX idx_journal_sku_id',
    'SELECT 1'
);
PREPARE drop_journal_sku_index_stmt FROM @drop_journal_sku_index;
EXECUTE drop_journal_sku_index_stmt;
DEALLOCATE PREPARE drop_journal_sku_index_stmt;
