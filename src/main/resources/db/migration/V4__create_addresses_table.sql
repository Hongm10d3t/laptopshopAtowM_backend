-- created_at/updated_at dùng DATETIME(6) như các bảng trước (V1/V2) — nhất
-- quán, tránh giới hạn năm 2038 của MySQL TIMESTAMP.
-- user_id không có UNIQUE: 1 user có thể có nhiều địa chỉ giao hàng.
-- is_default: chỉ tối đa 1 địa chỉ mặc định/user — ràng buộc này xử lý ở tầng
-- service (AddressService), không ép bằng constraint DB để tránh phức tạp hóa
-- (MySQL không có partial unique index như Postgres).

CREATE TABLE addresses (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    recipient_name  VARCHAR(255) NOT NULL,
    phone           VARCHAR(20)  NOT NULL,
    province        VARCHAR(255) NOT NULL,
    district        VARCHAR(255) NOT NULL,
    ward            VARCHAR(255) NOT NULL,
    street_address  VARCHAR(500) NOT NULL,
    is_default      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_addresses_user
        FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_addresses_user_id ON addresses (user_id);
