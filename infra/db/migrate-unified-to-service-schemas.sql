-- Migrate existing minimall_db data into service-owned schemas.
-- Run with services stopped and after every target schema has been created by Flyway.
-- Any duplicate, schema mismatch, count mismatch, checksum mismatch, or extra target row aborts.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
SET SESSION group_concat_max_len = 1048576;

CREATE TABLE IF NOT EXISTS minimall_db._schema_migration_audit (
    table_name VARCHAR(64) NOT NULL,
    source_rows BIGINT NOT NULL,
    target_rows BIGINT NOT NULL,
    source_checksum BIGINT NOT NULL,
    target_checksum BIGINT NOT NULL,
    PRIMARY KEY (table_name),
    CONSTRAINT chk_schema_migration_equal CHECK (
        source_rows = target_rows AND source_checksum = target_checksum
    )
) ENGINE=InnoDB;
TRUNCATE TABLE minimall_db._schema_migration_audit;

DROP PROCEDURE IF EXISTS minimall_db.compute_table_checksum;
DROP PROCEDURE IF EXISTS minimall_db.record_table_migration;
DELIMITER //
CREATE PROCEDURE minimall_db.compute_table_checksum(
    IN p_schema VARCHAR(64), IN p_table VARCHAR(64),
    OUT p_rows BIGINT, OUT p_checksum BIGINT)
BEGIN
    SET @column_expression = NULL;
    SELECT GROUP_CONCAT(
        CONCAT('IFNULL(CAST(`', column_name, '` AS CHAR), ''<NULL>'')')
        ORDER BY ordinal_position SEPARATOR ', '
    ) INTO @column_expression
    FROM information_schema.columns
    WHERE table_schema = p_schema AND table_name = p_table;

    SET @sql = CONCAT(
        'SELECT COUNT(*), COALESCE(SUM(CRC32(CONCAT_WS(''|'', ', @column_expression, '))), 0) ',
        'FROM `', p_schema, '`.`', p_table, '`');
    PREPARE checksum_stmt FROM @sql;
    EXECUTE checksum_stmt INTO p_rows, p_checksum;
    DEALLOCATE PREPARE checksum_stmt;
END//
CREATE PROCEDURE minimall_db.record_table_migration(
    IN p_source_schema VARCHAR(64), IN p_target_schema VARCHAR(64), IN p_table VARCHAR(64))
BEGIN
    CALL minimall_db.compute_table_checksum(
        p_source_schema, p_table, @source_rows, @source_checksum);
    CALL minimall_db.compute_table_checksum(
        p_target_schema, p_table, @target_rows, @target_checksum);
    INSERT INTO minimall_db._schema_migration_audit
        (table_name, source_rows, target_rows, source_checksum, target_checksum)
    VALUES (CONCAT(p_target_schema, '.', p_table),
        @source_rows, @target_rows, @source_checksum, @target_checksum);
END//
DELIMITER ;

START TRANSACTION;
DELETE FROM auth_db.t_auth_role WHERE id IN (1, 2, 3);
DELETE FROM id_db.t_id_segment;

-- auth_db.t_auth_role
INSERT INTO auth_db.`t_auth_role` (`id`, `role_code`, `role_name`, `description`, `status`, `version`, `is_deleted`, `create_time`, `update_time`)
SELECT `id`, `role_code`, `role_name`, `description`, `status`, `version`, `is_deleted`, `create_time`, `update_time` FROM minimall_db.`t_auth_role`;
CALL minimall_db.record_table_migration('minimall_db', 'auth_db', 't_auth_role');

-- auth_db.t_auth_permission
INSERT INTO auth_db.`t_auth_permission` (`id`, `permission_code`, `permission_name`, `resource_type`, `resource_path`, `description`, `status`, `version`, `is_deleted`, `create_time`, `update_time`)
SELECT `id`, `permission_code`, `permission_name`, `resource_type`, `resource_path`, `description`, `status`, `version`, `is_deleted`, `create_time`, `update_time` FROM minimall_db.`t_auth_permission`;
CALL minimall_db.record_table_migration('minimall_db', 'auth_db', 't_auth_permission');

-- auth_db.t_auth_user
INSERT INTO auth_db.`t_auth_user` (`id`, `account`, `phone_enc`, `email_enc`, `password_hash`, `salt`, `account_type`, `role_id`, `nickname`, `status`, `last_login_at`, `version`, `is_deleted`, `create_time`, `update_time`)
SELECT `id`, `account`, `phone_enc`, `email_enc`, `password_hash`, `salt`, `account_type`, `role_id`, `nickname`, `status`, `last_login_at`, `version`, `is_deleted`, `create_time`, `update_time` FROM minimall_db.`t_auth_user`;
CALL minimall_db.record_table_migration('minimall_db', 'auth_db', 't_auth_user');

-- auth_db.t_auth_role_permission
INSERT INTO auth_db.`t_auth_role_permission` (`id`, `role_id`, `permission_id`, `version`, `is_deleted`, `create_time`, `update_time`)
SELECT `id`, `role_id`, `permission_id`, `version`, `is_deleted`, `create_time`, `update_time` FROM minimall_db.`t_auth_role_permission`;
CALL minimall_db.record_table_migration('minimall_db', 'auth_db', 't_auth_role_permission');

-- auth_db.t_auth_token_blacklist
INSERT INTO auth_db.`t_auth_token_blacklist` (`id`, `jti`, `user_id`, `expires_at`, `revoked_at`, `reason`, `is_deleted`, `create_time`, `update_time`, `version`)
SELECT `id`, `jti`, `user_id`, `expires_at`, `revoked_at`, `reason`, `is_deleted`, `create_time`, `update_time`, `version` FROM minimall_db.`t_auth_token_blacklist`;
CALL minimall_db.record_table_migration('minimall_db', 'auth_db', 't_auth_token_blacklist');

-- user_db.t_user
INSERT INTO user_db.`t_user` (`id`, `username`, `nickname`, `phone`, `email`, `avatar`, `gender`, `phone_masked`, `version`, `is_deleted`, `create_time`, `update_time`)
SELECT `id`, `username`, `nickname`, `phone`, `email`, `avatar`, `gender`, `phone_masked`, `version`, `is_deleted`, `create_time`, `update_time` FROM minimall_db.`t_user`;
CALL minimall_db.record_table_migration('minimall_db', 'user_db', 't_user');

-- user_db.t_user_address
INSERT INTO user_db.`t_user_address` (`id`, `user_id`, `receiver_name`, `receiver_phone`, `province`, `city`, `district`, `detail_address`, `is_default`, `version`, `is_deleted`, `create_time`, `update_time`)
SELECT `id`, `user_id`, `receiver_name`, `receiver_phone`, `province`, `city`, `district`, `detail_address`, `is_default`, `version`, `is_deleted`, `create_time`, `update_time` FROM minimall_db.`t_user_address`;
CALL minimall_db.record_table_migration('minimall_db', 'user_db', 't_user_address');

-- user_db.t_user_favorite
INSERT INTO user_db.`t_user_favorite` (`id`, `user_id`, `sku_id`, `version`, `is_deleted`, `create_time`, `update_time`)
SELECT `id`, `user_id`, `sku_id`, `version`, `is_deleted`, `create_time`, `update_time` FROM minimall_db.`t_user_favorite`;
CALL minimall_db.record_table_migration('minimall_db', 'user_db', 't_user_favorite');

-- merchant_db.t_merch_merchant
INSERT INTO merchant_db.`t_merch_merchant` (`id`, `user_id`, `merchant_name`, `license_no`, `audit_status`, `audit_reason`, `audit_at`, `legal_person_enc`, `legal_phone_enc`, `id_card_no_enc`, `business_license_no_enc`, `bank_account_enc`, `version`, `is_deleted`, `create_time`, `update_time`)
SELECT `id`, `user_id`, `merchant_name`, `license_no`, `audit_status`, `audit_reason`, `audit_at`, `legal_person_enc`, `legal_phone_enc`, `id_card_no_enc`, `business_license_no_enc`, `bank_account_enc`, `version`, `is_deleted`, `create_time`, `update_time` FROM minimall_db.`t_merch_merchant`;
CALL minimall_db.record_table_migration('minimall_db', 'merchant_db', 't_merch_merchant');

-- merchant_db.t_merch_shop
INSERT INTO merchant_db.`t_merch_shop` (`id`, `merchant_id`, `shop_name`, `license_no`, `status`, `version`, `is_deleted`, `create_time`, `update_time`)
SELECT `id`, `merchant_id`, `shop_name`, `license_no`, `status`, `version`, `is_deleted`, `create_time`, `update_time` FROM minimall_db.`t_merch_shop`;
CALL minimall_db.record_table_migration('minimall_db', 'merchant_db', 't_merch_shop');

-- goods_db.t_goods_spu
INSERT INTO goods_db.`t_goods_spu` (`id`, `spu_no`, `merchant_id`, `shop_id`, `category_id`, `title`, `subtitle`, `main_image_url`, `status`, `publish_at`, `version`, `is_deleted`, `create_time`, `update_time`)
SELECT `id`, `spu_no`, `merchant_id`, `shop_id`, `category_id`, `title`, `subtitle`, `main_image_url`, `status`, `publish_at`, `version`, `is_deleted`, `create_time`, `update_time` FROM minimall_db.`t_goods_spu`;
CALL minimall_db.record_table_migration('minimall_db', 'goods_db', 't_goods_spu');

-- goods_db.t_sku
INSERT INTO goods_db.`t_sku` (`id`, `spu_id`, `merchant_id`, `sku_name`, `price`, `stock`, `version`, `is_deleted`, `create_time`, `update_time`)
SELECT `id`, `spu_id`, `merchant_id`, `sku_name`, `price`, `stock`, `version`, `is_deleted`, `create_time`, `update_time` FROM minimall_db.`t_sku`;
CALL minimall_db.record_table_migration('minimall_db', 'goods_db', 't_sku');

-- goods_db.t_goods_audit_record
INSERT INTO goods_db.`t_goods_audit_record` (`id`, `spu_id`, `auditor_id`, `decision`, `reason`, `audit_at`, `version`, `is_deleted`, `create_time`, `update_time`)
SELECT `id`, `spu_id`, `auditor_id`, `decision`, `reason`, `audit_at`, `version`, `is_deleted`, `create_time`, `update_time` FROM minimall_db.`t_goods_audit_record`;
CALL minimall_db.record_table_migration('minimall_db', 'goods_db', 't_goods_audit_record');

-- cart_db.t_cart_item
INSERT INTO cart_db.`t_cart_item` (`id`, `user_id`, `sku_id`, `quantity`, `selected`, `version`, `is_deleted`, `create_time`, `update_time`)
SELECT `id`, `user_id`, `sku_id`, `quantity`, `selected`, `version`, `is_deleted`, `create_time`, `update_time` FROM minimall_db.`t_cart_item`;
CALL minimall_db.record_table_migration('minimall_db', 'cart_db', 't_cart_item');

-- order_db.t_order
INSERT INTO order_db.`t_order` (`id`, `order_no`, `order_type`, `user_id`, `merchant_id`, `pay_id`, `sku_id`, `quantity`, `amount`, `refund_amount`, `total_refunded_amount`, `refund_request_no`, `total_amount`, `pay_amount`, `freight_amount`, `discount_amount`, `receiver_snapshot_json`, `pay_expire_at`, `paid_at`, `received_at`, `shipped_at`, `closed_at`, `close_reason`, `order_group_no`, `status`, `refund_from_status`, `client_ip`, `idempotency_key`, `version`, `is_deleted`, `create_time`, `update_time`)
SELECT `id`, `order_no`, `order_type`, `user_id`, `merchant_id`, `pay_id`, `sku_id`, `quantity`, `amount`, `refund_amount`, `total_refunded_amount`, `refund_request_no`, `total_amount`, `pay_amount`, `freight_amount`, `discount_amount`, `receiver_snapshot_json`, `pay_expire_at`, `paid_at`, `received_at`, `shipped_at`, `closed_at`, `close_reason`, `order_group_no`, `status`, `refund_from_status`, `client_ip`, `idempotency_key`, `version`, `is_deleted`, `create_time`, `update_time` FROM minimall_db.`t_order`;
CALL minimall_db.record_table_migration('minimall_db', 'order_db', 't_order');

-- order_db.t_order_item
INSERT INTO order_db.`t_order_item` (`id`, `order_id`, `spu_id`, `merchant_id`, `sku_id`, `spu_snapshot_json`, `sku_snapshot_json`, `sku_name`, `sku_image_url`, `quantity`, `unit_price`, `amount`, `subtotal_amount`, `discount_amount`, `pay_amount`, `refund_status`, `version`, `is_deleted`, `create_time`, `update_time`)
SELECT `id`, `order_id`, `spu_id`, `merchant_id`, `sku_id`, `spu_snapshot_json`, `sku_snapshot_json`, `sku_name`, `sku_image_url`, `quantity`, `unit_price`, `amount`, `subtotal_amount`, `discount_amount`, `pay_amount`, `refund_status`, `version`, `is_deleted`, `create_time`, `update_time` FROM minimall_db.`t_order_item`;
CALL minimall_db.record_table_migration('minimall_db', 'order_db', 't_order_item');

-- order_db.t_order_logistics
INSERT INTO order_db.`t_order_logistics` (`id`, `order_id`, `node`, `description`, `created_time`, `version`, `is_deleted`, `create_time`, `update_time`)
SELECT `id`, `order_id`, `node`, `description`, `created_time`, `version`, `is_deleted`, `create_time`, `update_time` FROM minimall_db.`t_order_logistics`;
CALL minimall_db.record_table_migration('minimall_db', 'order_db', 't_order_logistics');

-- order_db.t_order_status_log
INSERT INTO order_db.`t_order_status_log` (`id`, `order_id`, `from_status`, `to_status`, `trigger_source`, `operator_id`, `note`, `version`, `is_deleted`, `create_time`, `update_time`)
SELECT `id`, `order_id`, `from_status`, `to_status`, `trigger_source`, `operator_id`, `note`, `version`, `is_deleted`, `create_time`, `update_time` FROM minimall_db.`t_order_status_log`;
CALL minimall_db.record_table_migration('minimall_db', 'order_db', 't_order_status_log');

-- order_db.t_order_payment_snapshot
INSERT INTO order_db.`t_order_payment_snapshot` (`id`, `order_id`, `pay_amount`, `pay_method`, `expire_at`, `version`, `is_deleted`, `create_time`, `update_time`)
SELECT `id`, `order_id`, `pay_amount`, `pay_method`, `expire_at`, `version`, `is_deleted`, `create_time`, `update_time` FROM minimall_db.`t_order_payment_snapshot`;
CALL minimall_db.record_table_migration('minimall_db', 'order_db', 't_order_payment_snapshot');

-- order_db.t_order_outbox
INSERT INTO order_db.`t_order_outbox` (`id`, `transaction_id`, `version`, `is_deleted`, `create_time`, `update_time`)
SELECT `id`, `transaction_id`, `version`, `is_deleted`, `create_time`, `update_time` FROM minimall_db.`t_order_outbox`;
CALL minimall_db.record_table_migration('minimall_db', 'order_db', 't_order_outbox');

-- stock_db.t_stock
INSERT INTO stock_db.`t_stock` (`id`, `sku_id`, `merchant_id`, `available`, `reserved`, `alert_threshold`, `version`, `is_deleted`, `create_time`, `update_time`)
SELECT `id`, `sku_id`, `merchant_id`, `available`, `reserved`, `alert_threshold`, `version`, `is_deleted`, `create_time`, `update_time` FROM minimall_db.`t_stock`;
CALL minimall_db.record_table_migration('minimall_db', 'stock_db', 't_stock');

-- stock_db.t_stock_journal
INSERT INTO stock_db.`t_stock_journal` (`id`, `sku_id`, `quantity`, `type`, `reason`, `order_no`, `is_deleted`, `create_time`, `update_time`, `version`)
SELECT `id`, `sku_id`, `quantity`, `type`, `reason`, `order_no`, `is_deleted`, `create_time`, `update_time`, `version` FROM minimall_db.`t_stock_journal`;
CALL minimall_db.record_table_migration('minimall_db', 'stock_db', 't_stock_journal');

-- stock_db.t_stock_transfer
INSERT INTO stock_db.`t_stock_transfer` (`id`, `from_sku_id`, `to_sku_id`, `quantity`, `reason`, `operator_id`, `version`, `is_deleted`, `create_time`, `update_time`)
SELECT `id`, `from_sku_id`, `to_sku_id`, `quantity`, `reason`, `operator_id`, `version`, `is_deleted`, `create_time`, `update_time` FROM minimall_db.`t_stock_transfer`;
CALL minimall_db.record_table_migration('minimall_db', 'stock_db', 't_stock_transfer');

-- stock_db.t_stock_count_task
INSERT INTO stock_db.`t_stock_count_task` (`id`, `sku_id`, `expected_quantity`, `actual_quantity`, `diff_quantity`, `status`, `remark`, `operator_id`, `version`, `is_deleted`, `create_time`, `update_time`)
SELECT `id`, `sku_id`, `expected_quantity`, `actual_quantity`, `diff_quantity`, `status`, `remark`, `operator_id`, `version`, `is_deleted`, `create_time`, `update_time` FROM minimall_db.`t_stock_count_task`;
CALL minimall_db.record_table_migration('minimall_db', 'stock_db', 't_stock_count_task');

-- pay_db.t_pay_transaction
INSERT INTO pay_db.`t_pay_transaction` (`id`, `payment_no`, `trade_no`, `order_no`, `active_order_no`, `user_id`, `merchant_id`, `amount`, `currency`, `status`, `channel`, `channel_response`, `paid_at`, `expire_at`, `idempotency_key`, `version`, `is_deleted`, `create_time`, `update_time`)
SELECT `id`, `payment_no`, `trade_no`, `order_no`, `active_order_no`, `user_id`, `merchant_id`, `amount`, `currency`, `status`, `channel`, `channel_response`, `paid_at`, `expire_at`, `idempotency_key`, `version`, `is_deleted`, `create_time`, `update_time` FROM minimall_db.`t_pay_transaction`;
CALL minimall_db.record_table_migration('minimall_db', 'pay_db', 't_pay_transaction');

-- pay_db.t_pay_refund
INSERT INTO pay_db.`t_pay_refund` (`id`, `refund_no`, `refund_request_no`, `payment_no`, `refund_trade_no`, `amount`, `reason`, `status`, `notified_at`, `idempotency_key`, `retry_count`, `next_retry_at`, `version`, `is_deleted`, `create_time`, `update_time`)
SELECT `id`, `refund_no`, `refund_request_no`, `payment_no`, `refund_trade_no`, `amount`, `reason`, `status`, `notified_at`, `idempotency_key`, `retry_count`, `next_retry_at`, `version`, `is_deleted`, `create_time`, `update_time` FROM minimall_db.`t_pay_refund`;
CALL minimall_db.record_table_migration('minimall_db', 'pay_db', 't_pay_refund');

-- notify_db.t_notify_message
INSERT INTO notify_db.`t_notify_message` (`id`, `user_id`, `recipient_type`, `message_type`, `sender_id`, `biz_id`, `title`, `content`, `read_flag`, `version`, `is_deleted`, `create_time`, `update_time`)
SELECT `id`, `user_id`, `recipient_type`, `message_type`, `sender_id`, `biz_id`, `title`, `content`, `read_flag`, `version`, `is_deleted`, `create_time`, `update_time` FROM minimall_db.`t_notify_message`;
CALL minimall_db.record_table_migration('minimall_db', 'notify_db', 't_notify_message');

-- notify_db.t_notify_complaint
INSERT INTO notify_db.`t_notify_complaint` (`id`, `complainant_type`, `complainant_id`, `defendant_type`, `defendant_id`, `order_no`, `complaint_type`, `title`, `content`, `status`, `handler_id`, `handler_result`, `version`, `is_deleted`, `create_time`, `update_time`)
SELECT `id`, `complainant_type`, `complainant_id`, `defendant_type`, `defendant_id`, `order_no`, `complaint_type`, `title`, `content`, `status`, `handler_id`, `handler_result`, `version`, `is_deleted`, `create_time`, `update_time` FROM minimall_db.`t_notify_complaint`;
CALL minimall_db.record_table_migration('minimall_db', 'notify_db', 't_notify_complaint');

-- notify_db.t_notify_comment
INSERT INTO notify_db.`t_notify_comment` (`id`, `order_no`, `spu_id`, `sku_id`, `user_id`, `merchant_id`, `rating`, `content`, `images`, `anonymous`, `status`, `merchant_reply`, `merchant_reply_time`, `is_deleted`, `create_time`, `update_time`)
SELECT `id`, `order_no`, `spu_id`, `sku_id`, `user_id`, `merchant_id`, `rating`, `content`, `images`, `anonymous`, `status`, `merchant_reply`, `merchant_reply_time`, `is_deleted`, `create_time`, `update_time` FROM minimall_db.`t_notify_comment`;
CALL minimall_db.record_table_migration('minimall_db', 'notify_db', 't_notify_comment');

-- notify_db.t_notify_preference
INSERT INTO notify_db.`t_notify_preference` (`id`, `user_id`, `category_code`, `site_enabled`, `sms_enabled`, `email_enabled`, `is_deleted`, `create_time`, `update_time`)
SELECT `id`, `user_id`, `category_code`, `site_enabled`, `sms_enabled`, `email_enabled`, `is_deleted`, `create_time`, `update_time` FROM minimall_db.`t_notify_preference`;
CALL minimall_db.record_table_migration('minimall_db', 'notify_db', 't_notify_preference');

-- id_db.t_id_segment
INSERT INTO id_db.`t_id_segment` (`id`, `biz_tag`, `current_max`, `step`, `version`, `is_deleted`, `create_time`, `update_time`)
SELECT `id`, `biz_tag`, `current_max`, `step`, `version`, `is_deleted`, `create_time`, `update_time` FROM minimall_db.`t_id_segment`;
CALL minimall_db.record_table_migration('minimall_db', 'id_db', 't_id_segment');

COMMIT;

SELECT table_name, source_rows, target_rows, source_checksum, target_checksum
FROM minimall_db._schema_migration_audit
ORDER BY table_name;
DROP PROCEDURE minimall_db.compute_table_checksum;
DROP PROCEDURE minimall_db.record_table_migration;
SET FOREIGN_KEY_CHECKS = 1;
