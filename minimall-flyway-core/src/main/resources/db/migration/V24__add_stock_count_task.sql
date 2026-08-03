-- V24__add_stock_count_task.sql
-- 库存盘点任务表
CREATE TABLE IF NOT EXISTS t_stock_count_task (
  id BIGINT NOT NULL,
  sku_id BIGINT NOT NULL COMMENT '盘点SKU',
  expected_quantity BIGINT NOT NULL COMMENT '系统记录数量',
  actual_quantity BIGINT COMMENT '实际盘点数量（null=未盘点）',
  diff_quantity BIGINT COMMENT '差异数量（actual - expected）',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1=PENDING 2=COMPLETED 3=CANCELLED',
  remark VARCHAR(256) COMMENT '备注',
  operator_id BIGINT NOT NULL COMMENT '操作人',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT DEFAULT 0,
  PRIMARY KEY (id),
  INDEX idx_sku (sku_id),
  INDEX idx_status (status)
);
