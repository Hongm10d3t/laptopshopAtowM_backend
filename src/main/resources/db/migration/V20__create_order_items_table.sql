-- Snapshot tối thiểu theo DATABASE_DESIGN.md §2 (Order): product_name/
-- variant_name/sku/unit_price chụp lại tại thời điểm mua, không đọc lại từ
-- product_variants sau này. discount_amount mặc định 0 — chưa dùng tới voucher
-- (Giai đoạn 7), cột có sẵn để không phải migration thêm cột sau.

CREATE TABLE order_items (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    order_id            BIGINT        NOT NULL,
    product_variant_id  BIGINT        NOT NULL,
    product_name        VARCHAR(255)  NOT NULL,
    variant_name        VARCHAR(255)  NULL,
    sku                 VARCHAR(100)  NOT NULL,
    unit_price          DECIMAL(12,2) NOT NULL,
    quantity            INT           NOT NULL,
    discount_amount     DECIMAL(12,2) NOT NULL DEFAULT 0,
    created_at          DATETIME(6)   NOT NULL,
    updated_at          DATETIME(6)   NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT ck_order_items_quantity_positive CHECK (quantity > 0),
    CONSTRAINT ck_order_items_unit_price_non_negative CHECK (unit_price >= 0),
    CONSTRAINT ck_order_items_discount_non_negative CHECK (discount_amount >= 0),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_items_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_order_items_order ON order_items (order_id);
CREATE INDEX idx_order_items_variant ON order_items (product_variant_id);
