-- created_at/updated_at dùng DATETIME(6) thay vì TIMESTAMP để tránh giới hạn
-- năm 2038 của MySQL TIMESTAMP; kết nối JDBC đã cố định serverTimezone=UTC nên
-- DATETIME (không tự convert timezone) vẫn biểu diễn đúng java.time.Instant.
-- role/status lưu VARCHAR + CHECK constraint (thay vì MySQL ENUM) để khớp
-- @Enumerated(EnumType.STRING) chuẩn của JPA/Hibernate khi tạo entity sau.

CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    phone         VARCHAR(20)  NULL,
    role          VARCHAR(20)  NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'CUSTOMER')),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'BLOCKED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
