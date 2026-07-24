-- 1 user chỉ có 1 giỏ hàng (User 1--1 Cart, DATABASE_DESIGN.md §3) — UNIQUE
-- trên user_id. Giỏ được tạo lười (lazy) ở lần thêm sản phẩm đầu tiên, giống
-- tiền lệ InventoryBalance.

CREATE TABLE carts (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    user_id     BIGINT      NOT NULL,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uk_carts_user UNIQUE (user_id),
    CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
