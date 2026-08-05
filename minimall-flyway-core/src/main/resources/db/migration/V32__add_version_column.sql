-- V32: Add version column for MyBatis-Plus optimistic locking on all business tables.
-- The OptimisticLockerInnerInterceptor is already registered in MybatisPlusGlobalConfig.

ALTER TABLE t_auth_user ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version';
ALTER TABLE t_user ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version';
ALTER TABLE t_merch_shop ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version';
ALTER TABLE t_merch_merchant ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version';
ALTER TABLE t_sku ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version';
ALTER TABLE t_goods_spu ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version';
ALTER TABLE t_goods_audit_record ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version';
ALTER TABLE t_cart_item ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version';
ALTER TABLE t_stock ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version';
ALTER TABLE t_stock_transfer ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version';
ALTER TABLE t_stock_count_task ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version';
ALTER TABLE t_order ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version';
ALTER TABLE t_order_item ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version';
ALTER TABLE t_order_logistics ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version';
ALTER TABLE t_order_outbox ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version';
ALTER TABLE t_pay_transaction ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version';
ALTER TABLE t_pay_refund ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version';
ALTER TABLE t_merchant_withdraw ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version';
ALTER TABLE t_notify_message ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version';
ALTER TABLE t_notify_complaint ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version';
ALTER TABLE t_user_address ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version';
ALTER TABLE t_user_favorite ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version';
ALTER TABLE t_auth_role ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version';
ALTER TABLE t_auth_permission ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version';
ALTER TABLE t_auth_role_permission ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version';
ALTER TABLE t_auth_token_blacklist ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version';
-- t_id_segment 已在 V10 建表时声明 version INT DEFAULT 0，此处跳过避免 Duplicate column
