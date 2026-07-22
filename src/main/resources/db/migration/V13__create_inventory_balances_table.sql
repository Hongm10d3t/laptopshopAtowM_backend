-- inventory_balances không có warehouse_id — MVP chỉ 1 kho ngầm định (lệch
-- có chủ đích so với gợi ý "tạo bảng kho để dễ mở rộng" trong
-- DATABASE_DESIGN.md, xem ghi chú trong tài liệu đó). Không có cột version —
-- chống bán vượt tồn dùng update có điều kiện (@Modifying @Query ở
-- InventoryBalanceRepository), không dùng optimistic lock.

CREATE TABLE inventory_balances (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    product_variant_id  BIGINT      NOT NULL,
    on_hand_quantity    INT         NOT NULL DEFAULT 0,
    reserved_quantity   INT         NOT NULL DEFAULT 0,
    created_at          DATETIME(6) NOT NULL,
    updated_at          DATETIME(6) NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uk_inventory_balances_variant UNIQUE (product_variant_id),
    CONSTRAINT ck_inventory_balances_on_hand_non_negative CHECK (on_hand_quantity >= 0),
    CONSTRAINT ck_inventory_balances_reserved_non_negative CHECK (reserved_quantity >= 0),
    CONSTRAINT ck_inventory_balances_reserved_le_on_hand CHECK (reserved_quantity <= on_hand_quantity),
    CONSTRAINT fk_inventory_balances_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
