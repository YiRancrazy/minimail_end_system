ALTER TABLE t_merch_merchant
    ADD COLUMN license_image_url VARCHAR(512) NULL COMMENT '营业执照图片objectKey' AFTER license_no;