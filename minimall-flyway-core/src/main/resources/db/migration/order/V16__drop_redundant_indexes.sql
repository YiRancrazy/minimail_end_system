SET @order_item_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 't_order_item'
      AND index_name = 'idx_order'
);
SET @drop_order_item_index = IF(
    @order_item_index_exists > 0,
    'ALTER TABLE t_order_item DROP INDEX idx_order',
    'SELECT 1'
);
PREPARE drop_order_item_index_stmt FROM @drop_order_item_index;
EXECUTE drop_order_item_index_stmt;
DEALLOCATE PREPARE drop_order_item_index_stmt;
