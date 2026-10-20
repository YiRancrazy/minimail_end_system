ALTER TABLE t_notify_comment
    ADD UNIQUE KEY uk_notify_comment_order_spu_user (order_no, spu_id, user_id, is_deleted);
