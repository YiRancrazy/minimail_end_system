-- V27__add_user_auth_nickname.sql
-- t_user_auth 表新增 nickname 列
ALTER TABLE t_user_auth ADD COLUMN nickname VARCHAR(64) COMMENT '昵称';
