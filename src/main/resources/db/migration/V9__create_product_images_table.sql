-- Không có cột status: ảnh chỉ mang tính trình bày, không liên quan giao
-- dịch nên xóa cứng là đủ, không cần soft-deactivate như Category/Brand/
-- Product/ProductVariant.

CREATE TABLE product_images (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    product_id  BIGINT       NOT NULL,
    url         VARCHAR(500) NOT NULL,
    alt_text    VARCHAR(255) NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_product_images_product_sort ON product_images (product_id, sort_order);
