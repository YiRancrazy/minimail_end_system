ALTER TABLE t_notify_message
    MODIFY COLUMN read_flag INT NOT NULL DEFAULT 0 COMMENT '已读标志：0=未读 1=已读',
    ADD CONSTRAINT chk_notify_message_read_flag
        CHECK (read_flag IN (0, 1));

ALTER TABLE t_notify_complaint
    MODIFY COLUMN status TINYINT NOT NULL DEFAULT 0
        COMMENT '状态：0=待处理 1=处理中 2=已解决 3=驳回',
    ADD CONSTRAINT chk_notify_complaint_status
        CHECK (status BETWEEN 0 AND 3);

ALTER TABLE t_notify_comment
    MODIFY COLUMN anonymous TINYINT NOT NULL DEFAULT 0 COMMENT '是否匿名：0=否 1=是',
    MODIFY COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1=正常 2=隐藏 3=已删除',
    ADD CONSTRAINT chk_notify_comment_values
        CHECK (rating BETWEEN 1 AND 5 AND anonymous IN (0, 1) AND status BETWEEN 1 AND 3);

ALTER TABLE t_notify_preference
    MODIFY COLUMN site_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '站内信开关：0=关 1=开',
    MODIFY COLUMN sms_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '短信开关：0=关 1=开',
    MODIFY COLUMN email_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '邮件开关：0=关 1=开',
    ADD CONSTRAINT chk_notify_preference_flags CHECK (
        site_enabled IN (0, 1)
        AND sms_enabled IN (0, 1)
        AND email_enabled IN (0, 1)
    );
