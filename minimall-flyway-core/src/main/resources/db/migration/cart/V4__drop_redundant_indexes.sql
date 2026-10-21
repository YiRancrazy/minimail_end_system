SET @cart_user_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 't_cart_item'
      AND index_name = 'idx_user'
);
SET @drop_cart_user_index = IF(
    @cart_user_index_exists > 0,
    'ALTER TABLE t_cart_item DROP INDEX idx_user',
    'SELECT 1'
);
PREPARE drop_cart_user_index_stmt FROM @drop_cart_user_index;
EXECUTE drop_cart_user_index_stmt;
DEALLOCATE PREPARE drop_cart_user_index_stmt;
