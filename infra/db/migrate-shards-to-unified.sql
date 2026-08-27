-- ============================================================
-- 薄荷商城（mini_mail_system）分库 → 统一库迁移脚本
-- 用途：将原 11 个分库（auth_db / user_db / merchant_db / goods_db
--       / cart_db / stock_db / pay_db / order_db / notify_db / id_db）
--       中的历史数据迁移到 minimall_db
-- 前置：所有分库仍在同一 MySQL 实例中存在，且 minimall_db 已由
--       V1__init_minimall.sql 完成表结构初始化
-- 后置：迁移完成后可执行 DROP DATABASE auth_db; 等清理
-- 注意：使用 INSERT IGNORE 避免主键/唯一键冲突；建议先备份再执行
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 认证域（原 auth_db，5 张表）
-- ============================================================
INSERT IGNORE INTO minimall_db.t_auth_role             SELECT * FROM auth_db.t_auth_role;
INSERT IGNORE INTO minimall_db.t_auth_permission       SELECT * FROM auth_db.t_auth_permission;
INSERT IGNORE INTO minimall_db.t_auth_user             SELECT * FROM auth_db.t_auth_user;
INSERT IGNORE INTO minimall_db.t_auth_role_permission   SELECT * FROM auth_db.t_auth_role_permission;
INSERT IGNORE INTO minimall_db.t_auth_token_blacklist  SELECT * FROM auth_db.t_auth_token_blacklist;

-- ============================================================
-- 用户域（原 user_db，3 张表）
-- ============================================================
INSERT IGNORE INTO minimall_db.t_user                  SELECT * FROM user_db.t_user;
INSERT IGNORE INTO minimall_db.t_user_address           SELECT * FROM user_db.t_user_address;
INSERT IGNORE INTO minimall_db.t_user_favorite          SELECT * FROM user_db.t_user_favorite;

-- ============================================================
-- 商家域（原 merchant_db，3 张表）
-- ============================================================
INSERT IGNORE INTO minimall_db.t_merch_merchant         SELECT * FROM merchant_db.t_merch_merchant;
INSERT IGNORE INTO minimall_db.t_merch_shop            SELECT * FROM merchant_db.t_merch_shop;

-- ============================================================
-- 商品域（原 goods_db，3 张表）
-- ============================================================
INSERT IGNORE INTO minimall_db.t_goods_spu              SELECT * FROM goods_db.t_goods_spu;
INSERT IGNORE INTO minimall_db.t_sku                   SELECT * FROM goods_db.t_sku;
INSERT IGNORE INTO minimall_db.t_goods_audit_record     SELECT * FROM goods_db.t_goods_audit_record;

-- ============================================================
-- 交易域（原 cart_db + order_db，7 张表）
-- ============================================================
-- 购物车（原 cart_db）
INSERT IGNORE INTO minimall_db.t_cart_item             SELECT * FROM cart_db.t_cart_item;

-- 订单（原 order_db）
INSERT IGNORE INTO minimall_db.t_order                  SELECT * FROM order_db.t_order;
INSERT IGNORE INTO minimall_db.t_order_item            SELECT * FROM order_db.t_order_item;
INSERT IGNORE INTO minimall_db.t_order_logistics       SELECT * FROM order_db.t_order_logistics;
INSERT IGNORE INTO minimall_db.t_order_status_log      SELECT * FROM order_db.t_order_status_log;
INSERT IGNORE INTO minimall_db.t_order_payment_snapshot SELECT * FROM order_db.t_order_payment_snapshot;
INSERT IGNORE INTO minimall_db.t_order_outbox          SELECT * FROM order_db.t_order_outbox;

-- ============================================================
-- 库存域（原 stock_db，4 张表）
-- ============================================================
INSERT IGNORE INTO minimall_db.t_stock                  SELECT * FROM stock_db.t_stock;
INSERT IGNORE INTO minimall_db.t_stock_journal          SELECT * FROM stock_db.t_stock_journal;
INSERT IGNORE INTO minimall_db.t_stock_transfer         SELECT * FROM stock_db.t_stock_transfer;
INSERT IGNORE INTO minimall_db.t_stock_count_task      SELECT * FROM stock_db.t_stock_count_task;

-- ============================================================
-- 支付域（原 pay_db，2 张表）
-- ============================================================
INSERT IGNORE INTO minimall_db.t_pay_transaction        SELECT * FROM pay_db.t_pay_transaction;
INSERT IGNORE INTO minimall_db.t_pay_refund            SELECT * FROM pay_db.t_pay_refund;

-- ============================================================
-- 通知域（原 notify_db，4 张表）
-- ============================================================
INSERT IGNORE INTO minimall_db.t_notify_message         SELECT * FROM notify_db.t_notify_message;
INSERT IGNORE INTO minimall_db.t_notify_complaint       SELECT * FROM notify_db.t_notify_complaint;
INSERT IGNORE INTO minimall_db.t_notify_comment         SELECT * FROM notify_db.t_notify_comment;
INSERT IGNORE INTO minimall_db.t_notify_preference     SELECT * FROM notify_db.t_notify_preference;

-- ============================================================
-- ID 域（原 id_db，1 张表）
-- 注意：t_id_segment 已由 V1__init_minimall.sql 写入种子数据，
--       此处 INSERT IGNORE 仅在用户自定义号段时覆盖，不冲突
-- ============================================================
INSERT IGNORE INTO minimall_db.t_id_segment            SELECT * FROM id_db.t_id_segment;

-- 注意：undo_log 为 Seata AT 框架表，运行时生成，不迁移历史数据

SET FOREIGN_KEY_CHECKS = 1;
