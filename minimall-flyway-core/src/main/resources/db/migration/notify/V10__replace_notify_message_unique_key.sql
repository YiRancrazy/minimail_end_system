SET @notify_biz_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 't_notify_message'
      AND index_name = 'uk_notify_biz'
);
SET @add_notify_biz_index = IF(
    @notify_biz_index_exists = 0,
    'ALTER TABLE t_notify_message ADD UNIQUE KEY uk_notify_biz '
        ' (recipient_type, user_id, message_type, biz_id, is_deleted)',
    'SELECT 1'
);
PREPARE add_notify_biz_index_stmt FROM @add_notify_biz_index;
EXECUTE add_notify_biz_index_stmt;
DEALLOCATE PREPARE add_notify_biz_index_stmt;

SET @notify_unread_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 't_notify_message'
      AND index_name = 'uk_notify_message_receiver_unread'
);
SET @drop_notify_unread_index = IF(
    @notify_unread_index_exists > 0,
    'ALTER TABLE t_notify_message DROP INDEX uk_notify_message_receiver_unread',
    'SELECT 1'
);
PREPARE drop_notify_unread_index_stmt FROM @drop_notify_unread_index;
EXECUTE drop_notify_unread_index_stmt;
DEALLOCATE PREPARE drop_notify_unread_index_stmt;
