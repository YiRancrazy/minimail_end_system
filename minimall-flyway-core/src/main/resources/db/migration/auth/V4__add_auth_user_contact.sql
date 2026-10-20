-- 平台管理员联系方式字段：仅平台管理员使用，用户/商家为空
ALTER TABLE t_auth_user
    ADD COLUMN contact VARCHAR(64) NULL COMMENT '联系方式（平台管理员使用）';