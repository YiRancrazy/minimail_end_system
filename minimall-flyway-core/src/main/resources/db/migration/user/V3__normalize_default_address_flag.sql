UPDATE t_user_address
SET is_default = NULL
WHERE is_default = 0;

ALTER TABLE t_user_address
    MODIFY COLUMN is_default TINYINT NULL DEFAULT NULL
        COMMENT '是否默认地址：1=是 NULL=否';
