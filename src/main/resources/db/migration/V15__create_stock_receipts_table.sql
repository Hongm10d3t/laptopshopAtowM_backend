CREATE TABLE stock_receipts (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    code                 VARCHAR(50)  NOT NULL,
    status               VARCHAR(20)  NOT NULL,
    note                 VARCHAR(500) NULL,
    created_by_user_id   BIGINT       NOT NULL,
    confirmed_by_user_id BIGINT       NULL,
    confirmed_at         DATETIME(6)  NULL,
    cancelled_by_user_id BIGINT       NULL,
    cancelled_at         DATETIME(6)  NULL,
    created_at           DATETIME(6)  NOT NULL,
    updated_at           DATETIME(6)  NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uk_stock_receipts_code UNIQUE (code),
    CONSTRAINT ck_stock_receipts_status CHECK (status IN ('DRAFT', 'CONFIRMED', 'CANCELLED')),
    CONSTRAINT fk_stock_receipts_created_by FOREIGN KEY (created_by_user_id) REFERENCES users (id),
    CONSTRAINT fk_stock_receipts_confirmed_by FOREIGN KEY (confirmed_by_user_id) REFERENCES users (id),
    CONSTRAINT fk_stock_receipts_cancelled_by FOREIGN KEY (cancelled_by_user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_stock_receipts_status ON stock_receipts (status);
