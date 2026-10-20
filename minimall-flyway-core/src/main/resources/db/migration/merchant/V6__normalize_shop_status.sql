UPDATE t_merch_shop
SET status = CASE UPPER(CAST(status AS CHAR))
    WHEN 'ACTIVE' THEN 1
    WHEN 'PENDING' THEN 2
    WHEN 'CLOSED' THEN 2
    ELSE status
END;

ALTER TABLE t_merch_shop
    MODIFY COLUMN status TINYINT NOT NULL DEFAULT 1
        COMMENT '状态：1=营业中 2=已停业 3=已冻结';
