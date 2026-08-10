-- H2 schema for pay-service integration tests (test profile).
-- Mirrors the MySQL DDL used in dev/prod. Mode=MySQL ensures compatible column types.

CREATE TABLE IF NOT EXISTS "t_pay_transaction" (
    "id"                BIGINT       NOT NULL PRIMARY KEY,
    "payment_no"        VARCHAR(64)  NOT NULL,
    "trade_no"          VARCHAR(64),
    "order_no"          VARCHAR(64),
    "user_id"           BIGINT,
    "merchant_id"       BIGINT,
    "amount"            DECIMAL(12,2),
    "currency"          VARCHAR(8),
    "status"            INT,
    "channel"           INT,
    "channel_response"  VARCHAR(512),
    "paid_at"           TIMESTAMP,
    "expire_at"         TIMESTAMP,
    "idempotency_key"   VARCHAR(64),
    "create_time"       TIMESTAMP,
    "update_time"       TIMESTAMP,
    "is_deleted"        INT DEFAULT 0,
    "version"           INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS "t_pay_refund" (
    "id"                BIGINT       NOT NULL PRIMARY KEY,
    "refund_no"         VARCHAR(64)  NOT NULL,
    "payment_no"        VARCHAR(64),
    "refund_trade_no"   VARCHAR(64),
    "amount"            DECIMAL(12,2),
    "reason"            VARCHAR(255),
    "status"            INT,
    "notified_at"       TIMESTAMP,
    "idempotency_key"   VARCHAR(64),
    "create_time"       TIMESTAMP,
    "update_time"       TIMESTAMP,
    "is_deleted"        INT DEFAULT 0,
    "version"           INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS "t_merchant_withdraw" (
    "id"                BIGINT       NOT NULL PRIMARY KEY,
    "merchant_id"       BIGINT,
    "withdraw_no"       VARCHAR(64),
    "amount"            DECIMAL(12,2),
    "status"            INT,
    "reason"            VARCHAR(255),
    "applied_at"        TIMESTAMP,
    "reviewed_at"       TIMESTAMP,
    "create_time"       TIMESTAMP,
    "update_time"       TIMESTAMP,
    "is_deleted"        INT DEFAULT 0,
    "version"           INT DEFAULT 0
);
