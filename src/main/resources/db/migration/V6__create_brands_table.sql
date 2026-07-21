-- Cùng dạng với categories (V5): phẳng, status ACTIVE/INACTIVE.
-- logo_url chỉ lưu URL đã hosted sẵn — không có upload/storage ảnh ở giai
-- đoạn này.

CREATE TABLE brands (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    name        VARCHAR(150)  NOT NULL,
    slug        VARCHAR(160)  NOT NULL,
    description VARCHAR(1000) NULL,
    logo_url    VARCHAR(500)  NULL,
    status      VARCHAR(20)   NOT NULL,
    created_at  DATETIME(6)   NOT NULL,
    updated_at  DATETIME(6)   NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uk_brands_slug UNIQUE (slug),
    CONSTRAINT ck_brands_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_brands_status ON brands (status);
