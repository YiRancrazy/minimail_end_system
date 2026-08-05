-- Add shipped_at column to t_order for auto-confirm scheduling
ALTER TABLE t_order ADD COLUMN shipped_at DATETIME NULL COMMENT '发货时间，用于自动确认收货判定';
