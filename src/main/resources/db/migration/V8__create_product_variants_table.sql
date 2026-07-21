-- ram_gb/storage_gb/storage_type/color là cột tường minh (không EAV) — tập
-- thuộc tính hữu hạn quyết định SKU khác nhau, khác thông số kỹ thuật tự do
-- của Product (bảng specifications/product_spec_values ở migration sau).

CREATE TABLE product_variants (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    product_id    BIGINT        NOT NULL,
    sku           VARCHAR(100)  NOT NULL,
    variant_name  VARCHAR(255)  NULL,
    price         DECIMAL(12,2) NOT NULL,
    ram_gb        INT           NULL,
    storage_gb    INT           NULL,
    storage_type  VARCHAR(20)   NULL,
    color         VARCHAR(50)   NULL,
    status        VARCHAR(20)   NOT NULL,
    created_at    DATETIME(6)   NOT NULL,
    updated_at    DATETIME(6)   NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uk_product_variants_sku UNIQUE (sku),
    CONSTRAINT ck_product_variants_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_product_variants_price CHECK (price >= 0),
    CONSTRAINT fk_product_variants_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_product_variants_product ON product_variants (product_id);
CREATE INDEX idx_product_variants_price ON product_variants (price);
CREATE INDEX idx_product_variants_status ON product_variants (status);
