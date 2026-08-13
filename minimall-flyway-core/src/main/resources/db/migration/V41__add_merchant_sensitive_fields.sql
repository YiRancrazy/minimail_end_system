-- 商家敏感字段加密落地（数据库设计文档 §18），密文以 Base64 存 VARCHAR，与 t_user.phone 方案一致
ALTER TABLE t_merch_merchant
  ADD COLUMN legal_person_enc VARCHAR(512) NULL COMMENT '法人姓名 AES-256-GCM 密文',
  ADD COLUMN legal_phone_enc VARCHAR(512) NULL COMMENT '法人手机 AES-256-GCM 密文',
  ADD COLUMN id_card_no_enc VARCHAR(512) NULL COMMENT '身份证号 AES-256-GCM 密文',
  ADD COLUMN business_license_no_enc VARCHAR(512) NULL COMMENT '营业执照号 AES-256-GCM 密文',
  ADD COLUMN bank_account_enc VARCHAR(512) NULL COMMENT '提现银行卡 AES-256-GCM 密文';

-- 存量 license_no 明文保留（兼容旧数据）；新数据统一写加密列。prod 存量迁移需应用层脚本（见实施计划 Task 3）。
