-- category_id NULL = spec áp dụng toàn cục (mọi danh mục); có giá trị = chỉ
-- áp dụng cho 1 danh mục cụ thể. Dữ liệu tham chiếu ít thay đổi, chỉ đọc ở
-- giai đoạn này (không có Admin CRUD) — seed baseline ở V12.
-- Không tách bảng "specification_groups" riêng — group_label chỉ để gom
-- nhóm hiển thị, không cần bảng thứ 3 cho mục đích thuần cosmetic này.

CREATE TABLE specifications (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    category_id   BIGINT       NULL,
    code          VARCHAR(100) NOT NULL,
    label         VARCHAR(150) NOT NULL,
    unit          VARCHAR(20)  NULL,
    group_label   VARCHAR(100) NULL,
    display_order INT          NOT NULL DEFAULT 0,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uk_specifications_code UNIQUE (code),
    CONSTRAINT fk_specifications_category FOREIGN KEY (category_id) REFERENCES categories (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_specifications_category ON specifications (category_id);
