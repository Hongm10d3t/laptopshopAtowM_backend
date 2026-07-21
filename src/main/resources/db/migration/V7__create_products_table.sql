-- category_id/brand_id không có ON DELETE CASCADE: Category/Brand chỉ
-- ngừng hoạt động bằng status, không xóa cứng (PROJECT_RULES.md mục 4), nên
-- không cần xử lý xóa lan truyền.

CREATE TABLE products (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    category_id        BIGINT        NOT NULL,
    brand_id           BIGINT        NOT NULL,
    name               VARCHAR(255)  NOT NULL,
    slug               VARCHAR(280)  NOT NULL,
    short_description  VARCHAR(500)  NULL,
    description        TEXT          NULL,
    status             VARCHAR(20)   NOT NULL,
    created_at         DATETIME(6)   NOT NULL,
    updated_at         DATETIME(6)   NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uk_products_slug UNIQUE (slug),
    CONSTRAINT ck_products_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT fk_products_brand FOREIGN KEY (brand_id) REFERENCES brands (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_products_category ON products (category_id);
CREATE INDEX idx_products_brand ON products (brand_id);
CREATE INDEX idx_products_status ON products (status);
CREATE INDEX idx_products_name ON products (name);
