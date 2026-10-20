ALTER TABLE t_notify_message
    ADD UNIQUE KEY uk_notify_biz (recipient_type, user_id, message_type, biz_id, is_deleted);

ALTER TABLE t_notify_message
    DROP INDEX uk_notify_message_receiver_unread;
