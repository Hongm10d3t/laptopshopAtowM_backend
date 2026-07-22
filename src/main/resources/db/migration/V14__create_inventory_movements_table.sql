-- reference_type/reference_id là tham chiếu polymorphic không có FK: movement
-- có thể trỏ tới stock_receipts (RECEIPT), hoặc tới orders/order_items ở giai
-- đoạn sau (RESERVE/RELEASE/SHIPMENT/RETURN) — module inventory không được
-- phụ thuộc ngược vào bảng của module order chưa tồn tại (PROJECT_RULES §2).
-- on_hand_after/reserved_after là snapshot số dư SAU khi áp dụng, phục vụ
-- audit mà không cần join lại inventory_balances tại thời điểm lịch sử.

CREATE TABLE inventory_movements (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    product_variant_id  BIGINT       NOT NULL,
    type                VARCHAR(20)  NOT NULL,
    quantity            INT          NOT NULL,
    on_hand_after       INT          NOT NULL,
    reserved_after      INT          NOT NULL,
    reference_type      VARCHAR(50)  NULL,
    reference_id        BIGINT       NULL,
    reason              VARCHAR(255) NULL,
    created_by_user_id  BIGINT       NULL,
    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT ck_inventory_movements_type
        CHECK (type IN ('RECEIPT', 'RESERVE', 'RELEASE', 'SHIPMENT', 'RETURN', 'ADJUSTMENT_IN', 'ADJUSTMENT_OUT')),
    CONSTRAINT ck_inventory_movements_quantity_positive CHECK (quantity > 0),
    CONSTRAINT fk_inventory_movements_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants (id),
    CONSTRAINT fk_inventory_movements_created_by FOREIGN KEY (created_by_user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_inventory_movements_variant ON inventory_movements (product_variant_id);
CREATE INDEX idx_inventory_movements_type ON inventory_movements (type);
CREATE INDEX idx_inventory_movements_reference ON inventory_movements (reference_type, reference_id);
