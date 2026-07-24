-- Immutable, append-only: ghi 1 dòng mỗi lần Order đổi trạng thái (confirm/
-- prepare/ship/deliver/cancel/request-return/approve-or-reject-return). Thay
-- thế audit-column-per-transition kiểu stock_receipts (confirmed_by_user_id/
-- confirmed_at, cancelled_by_user_id/cancelled_at) vì Order có tới 7
-- transition — dồn vào 1 bảng gọn hơn nhiều so với rải cột trên orders.
-- from_status luôn NOT NULL: dòng lịch sử chỉ ghi khi có transition THẬT SỰ
-- xảy ra, không ghi cho lúc tạo đơn (PENDING ban đầu đã có orders.created_at).

CREATE TABLE order_status_history (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    order_id            BIGINT       NOT NULL,
    from_status         VARCHAR(20)  NOT NULL,
    to_status           VARCHAR(20)  NOT NULL,
    changed_by_user_id  BIGINT       NOT NULL,
    note                VARCHAR(500) NULL,
    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_order_status_history_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_status_history_changed_by FOREIGN KEY (changed_by_user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_order_status_history_order ON order_status_history (order_id);
