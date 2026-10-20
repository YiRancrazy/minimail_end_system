-- t_notify_comment 与 t_notify_preference 在初版建表时遗漏了乐观锁 version 列，
-- 而对应实体继承 BasePO（含 @Version 注解），MyBatis-Plus 查询会强制带上该列，
-- 导致运行时 SQLSyntaxErrorException Unknown column 'version'。此处补列对齐其余表。
ALTER TABLE t_notify_comment
    ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本' AFTER update_time;

ALTER TABLE t_notify_preference
    ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本' AFTER update_time;