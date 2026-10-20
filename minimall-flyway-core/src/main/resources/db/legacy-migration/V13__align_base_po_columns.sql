-- 统一实体公共列名：库存流水时间列改为 create_time/update_time 并补乐观锁版本；
-- 令牌黑名单补 BasePO 主键与审计列，jti 降级为业务唯一键。

ALTER TABLE t_stock_journal
    CHANGE COLUMN created_at create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    CHANGE COLUMN updated_at update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本';

ALTER TABLE t_auth_token_blacklist
    DROP PRIMARY KEY,
    ADD COLUMN id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT FIRST COMMENT '主键ID',
    ADD COLUMN is_deleted TINYINT DEFAULT 0 COMMENT '是否删除：0=未删除 1=已删除（允许NULL）',
    ADD COLUMN create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    ADD COLUMN update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    ADD PRIMARY KEY (id),
    ADD UNIQUE KEY uk_token_bl_jti (jti);
