-- UNIQUE(product_id, specification_id): 1 sản phẩm chỉ có tối đa 1 giá trị
-- cho mỗi thông số kỹ thuật.

CREATE TABLE product_spec_values (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    product_id        BIGINT       NOT NULL,
    specification_id  BIGINT       NOT NULL,
    value             VARCHAR(500) NOT NULL,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uk_product_spec_values UNIQUE (product_id, specification_id),
    CONSTRAINT fk_psv_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_psv_specification FOREIGN KEY (specification_id) REFERENCES specifications (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_psv_specification ON product_spec_values (specification_id);
