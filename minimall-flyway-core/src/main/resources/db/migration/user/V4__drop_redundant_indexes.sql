SET @address_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 't_user_address'
      AND index_name = 'idx_user_id'
);
SET @drop_address_index = IF(
    @address_index_exists > 0,
    'ALTER TABLE t_user_address DROP INDEX idx_user_id',
    'SELECT 1'
);
PREPARE drop_address_index_stmt FROM @drop_address_index;
EXECUTE drop_address_index_stmt;
DEALLOCATE PREPARE drop_address_index_stmt;

SET @favorite_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 't_user_favorite'
      AND index_name = 'idx_user_favorite_user_id'
);
SET @drop_favorite_index = IF(
    @favorite_index_exists > 0,
    'ALTER TABLE t_user_favorite DROP INDEX idx_user_favorite_user_id',
    'SELECT 1'
);
PREPARE drop_favorite_index_stmt FROM @drop_favorite_index;
EXECUTE drop_favorite_index_stmt;
DEALLOCATE PREPARE drop_favorite_index_stmt;
