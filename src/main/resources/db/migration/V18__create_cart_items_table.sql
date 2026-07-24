-- Không lưu price ở cart_items — giá luôn đọc live từ product_variants khi
-- hiển thị giỏ hàng lẫn lúc checkout (PROJECT_RULES.md §6: "Checkout phải đọc
-- lại giá... Frontend không quyết định tổng tiền cuối cùng"), tránh giỏ hàng
-- giữ giá cũ. Thêm giỏ không giữ tồn (PROJECT_RULES.md §5).

CREATE TABLE cart_items (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    cart_id             BIGINT      NOT NULL,
    product_variant_id  BIGINT      NOT NULL,
    quantity            INT         NOT NULL,
    created_at          DATETIME(6) NOT NULL,
    updated_at          DATETIME(6) NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uk_cart_items_cart_variant UNIQUE (cart_id, product_variant_id),
    CONSTRAINT ck_cart_items_quantity_positive CHECK (quantity > 0),
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts (id),
    CONSTRAINT fk_cart_items_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_cart_items_cart ON cart_items (cart_id);
