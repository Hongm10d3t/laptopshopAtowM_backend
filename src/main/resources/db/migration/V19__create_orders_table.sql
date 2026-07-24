-- status khai đủ toàn bộ state machine ở PROJECT_RULES.md §6 ngay từ đầu dù
-- Giai đoạn 5 chỉ tạo được PENDING — Giai đoạn 6 sẽ thêm transition, không
-- cần sửa lại CHECK constraint này.
-- payment_method: chỉ COD thực sự dùng được ở Giai đoạn 5 (enforce ở tầng
-- entity), ONLINE để sẵn cho Giai đoạn 7.
-- recipient_name..street_address là snapshot địa chỉ giao hàng tại thời điểm
-- đặt đơn (không FK tới addresses) — địa chỉ gốc có thể bị Customer sửa/xoá
-- sau đó, đơn phải giữ đúng dữ liệu lúc mua.

CREATE TABLE orders (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    user_id         BIGINT        NOT NULL,
    status          VARCHAR(20)   NOT NULL,
    payment_method  VARCHAR(20)   NOT NULL,
    total_amount    DECIMAL(12,2) NOT NULL,
    note            VARCHAR(500)  NULL,
    recipient_name  VARCHAR(255)  NOT NULL,
    phone           VARCHAR(20)   NOT NULL,
    province        VARCHAR(255)  NOT NULL,
    district        VARCHAR(255)  NOT NULL,
    ward            VARCHAR(255)  NOT NULL,
    street_address  VARCHAR(500)  NOT NULL,
    created_at      DATETIME(6)   NOT NULL,
    updated_at      DATETIME(6)   NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT ck_orders_status CHECK (status IN
        ('PENDING', 'CONFIRMED', 'PREPARING', 'SHIPPING', 'DELIVERED', 'CANCELLED',
         'RETURN_REQUESTED', 'RETURNED')),
    CONSTRAINT ck_orders_payment_method CHECK (payment_method IN ('COD', 'ONLINE')),
    CONSTRAINT ck_orders_total_amount_non_negative CHECK (total_amount >= 0),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_orders_user_id ON orders (user_id);
CREATE INDEX idx_orders_status ON orders (status);
