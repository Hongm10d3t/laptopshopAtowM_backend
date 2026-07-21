-- created_at/updated_at dùng DATETIME(6) như các bảng trước (V1-V4) — nhất
-- quán, tránh giới hạn năm 2038 của MySQL TIMESTAMP.
-- Phẳng, không có parent_id: chưa có yêu cầu danh mục nhiều cấp ở MVP.

CREATE TABLE categories (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    name        VARCHAR(150)  NOT NULL,
    slug        VARCHAR(160)  NOT NULL,
    description VARCHAR(1000) NULL,
    status      VARCHAR(20)   NOT NULL,
    created_at  DATETIME(6)   NOT NULL,
    updated_at  DATETIME(6)   NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uk_categories_slug UNIQUE (slug),
    CONSTRAINT ck_categories_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_categories_status ON categories (status);
