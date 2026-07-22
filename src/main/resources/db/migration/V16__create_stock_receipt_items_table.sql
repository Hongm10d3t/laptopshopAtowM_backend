CREATE TABLE stock_receipt_items (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    stock_receipt_id    BIGINT      NOT NULL,
    product_variant_id  BIGINT      NOT NULL,
    quantity            INT         NOT NULL,
    created_at          DATETIME(6) NOT NULL,
    updated_at          DATETIME(6) NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uk_stock_receipt_items_receipt_variant UNIQUE (stock_receipt_id, product_variant_id),
    CONSTRAINT ck_stock_receipt_items_quantity_positive CHECK (quantity > 0),
    CONSTRAINT fk_stock_receipt_items_receipt FOREIGN KEY (stock_receipt_id) REFERENCES stock_receipts (id),
    CONSTRAINT fk_stock_receipt_items_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_stock_receipt_items_receipt ON stock_receipt_items (stock_receipt_id);
CREATE INDEX idx_stock_receipt_items_variant ON stock_receipt_items (product_variant_id);
