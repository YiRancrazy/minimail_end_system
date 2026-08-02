CREATE TABLE IF NOT EXISTS t_goods_audit_record (
  id BIGINT NOT NULL,
  spu_id BIGINT NOT NULL,
  auditor_id BIGINT NOT NULL,
  decision TINYINT NOT NULL COMMENT '1=通过 2=驳回',
  reason VARCHAR(512),
  audit_at DATETIME(3),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_goods_audit_spu (spu_id, audit_at)
);
