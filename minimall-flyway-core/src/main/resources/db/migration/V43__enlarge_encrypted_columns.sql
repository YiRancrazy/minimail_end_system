-- V43__enlarge_encrypted_columns.sql
-- 字段级 AES-256-GCM 加密列宽度不足：11 位手机号密文约 52 字符，邮箱/详细地址密文更长，
-- 原 VARCHAR(32/128/64/20/256) 在严格模式写入报错、非严格模式截断导致解密失败，统一扩为 VARCHAR(512)。
-- t_merch_merchant.*_enc 已为 VARCHAR(512)，t_pay_transaction.channel_response 为 TEXT，无需调整。
ALTER TABLE t_user MODIFY COLUMN phone VARCHAR(512) COMMENT '手机号，AES加密存储';
ALTER TABLE t_user MODIFY COLUMN email VARCHAR(512) COMMENT '邮箱，AES加密存储';
ALTER TABLE t_user_address MODIFY COLUMN receiver_name VARCHAR(512) NOT NULL COMMENT '收货人姓名，AES加密存储';
ALTER TABLE t_user_address MODIFY COLUMN receiver_phone VARCHAR(512) NOT NULL COMMENT '收货人手机号，AES加密存储';
ALTER TABLE t_user_address MODIFY COLUMN detail_address VARCHAR(512) NOT NULL COMMENT '详细地址，AES加密存储';
