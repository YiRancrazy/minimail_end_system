DROP TABLE IF EXISTS t_order_outbox;
DROP TABLE IF EXISTS t_order_status_log;
DROP TABLE IF EXISTS t_order;

CREATE TABLE t_order (
    id                      BIGINT          NOT NULL,
    order_no                VARCHAR(32)     NOT NULL,
    order_type              TINYINT         NOT NULL,
    user_id                 BIGINT          NOT NULL,
    merchant_id             BIGINT          NULL,
    pay_id                  BIGINT          NULL,
    sku_id                  BIGINT          NULL,
    quantity                INT             NULL,
    amount                  DECIMAL(12,2)   NULL,
    refund_amount           DECIMAL(12,2)   NULL,
    total_amount            DECIMAL(12,2)   NOT NULL,
    pay_amount              DECIMAL(12,2)   NOT NULL,
    freight_amount          DECIMAL(12,2)   NOT NULL,
    discount_amount         DECIMAL(12,2)   NOT NULL,
    receiver_snapshot_json  CLOB            NULL,
    pay_expire_at           TIMESTAMP       NULL,
    paid_at                 TIMESTAMP       NULL,
    received_at             TIMESTAMP       NULL,
    shipped_at              TIMESTAMP       NULL,
    closed_at               TIMESTAMP       NULL,
    close_reason            VARCHAR(128)    NULL,
    order_group_no          VARCHAR(32)     NULL,
    status                  TINYINT         NOT NULL,
    refund_from_status      TINYINT         NULL,
    client_ip               VARCHAR(45)     NULL,
    idempotency_key         VARCHAR(36)     NULL,
    stock_release_status    TINYINT         NOT NULL,
    refund_request_no       VARCHAR(64)     NULL,
    total_refunded_amount   DECIMAL(12,2)   NULL,
    version                 INT             NOT NULL DEFAULT 0,
    is_deleted              TINYINT         DEFAULT 0,
    create_time             TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    update_time             TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE t_order_status_log (
    id              BIGINT          NOT NULL,
    order_id        BIGINT          NOT NULL,
    from_status     TINYINT         NULL,
    to_status       TINYINT         NOT NULL,
    order_version   INT             NULL,
    trigger_source  VARCHAR(32)     NOT NULL,
    operator_id     BIGINT          NULL,
    note            VARCHAR(255)    NULL,
    version         INT             NOT NULL DEFAULT 0,
    is_deleted      TINYINT         DEFAULT 0,
    create_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE t_order_outbox (
    id              BIGINT          NOT NULL,
    event_id        VARCHAR(128)    NOT NULL,
    event_type      VARCHAR(64)     NULL,
    aggregate_type  VARCHAR(32)     NULL,
    aggregate_id    BIGINT          NULL,
    topic           VARCHAR(128)    NULL,
    tag             VARCHAR(128)    NULL,
    payload         LONGTEXT        NULL,
    send_status     TINYINT         NOT NULL DEFAULT 0,
    retry_count     INT             NOT NULL DEFAULT 0,
    next_retry_time TIMESTAMP       NULL,
    last_error      VARCHAR(512)    NULL,
    sent_time       TIMESTAMP       NULL,
    version         INT             NOT NULL DEFAULT 0,
    is_deleted      TINYINT         DEFAULT 0,
    create_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (event_id),
    CONSTRAINT chk_order_outbox_route
        CHECK (payload IS NULL OR (topic IS NOT NULL AND tag IS NOT NULL))
);
