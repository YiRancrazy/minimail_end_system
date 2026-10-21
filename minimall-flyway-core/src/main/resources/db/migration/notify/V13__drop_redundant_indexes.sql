SET @notify_comment_order_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 't_notify_comment'
      AND index_name = 'idx_notify_comment_order'
);
SET @drop_notify_comment_order_index = IF(
    @notify_comment_order_index_exists > 0,
    'ALTER TABLE t_notify_comment DROP INDEX idx_notify_comment_order',
    'SELECT 1'
);
PREPARE drop_notify_comment_order_index_stmt FROM @drop_notify_comment_order_index;
EXECUTE drop_notify_comment_order_index_stmt;
DEALLOCATE PREPARE drop_notify_comment_order_index_stmt;

SET @notify_preference_user_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 't_notify_preference'
      AND index_name = 'idx_notify_preference_user'
);
SET @drop_notify_preference_user_index = IF(
    @notify_preference_user_index_exists > 0,
    'ALTER TABLE t_notify_preference DROP INDEX idx_notify_preference_user',
    'SELECT 1'
);
PREPARE drop_notify_preference_user_index_stmt FROM @drop_notify_preference_user_index;
EXECUTE drop_notify_preference_user_index_stmt;
DEALLOCATE PREPARE drop_notify_preference_user_index_stmt;
