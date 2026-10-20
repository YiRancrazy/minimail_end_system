ALTER TABLE t_order
    ADD COLUMN stock_release_status TINYINT NOT NULL DEFAULT 0
        COMMENT '库存释放状态：0=无需释放 1=待释放 2=已释放' AFTER status;
