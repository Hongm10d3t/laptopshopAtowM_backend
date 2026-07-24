package com.laptophub.order.entity;

// Đủ toàn bộ state machine ở PROJECT_RULES.md §6 ngay từ Giai đoạn 5 dù chỉ
// PENDING đạt được ở giai đoạn này — Giai đoạn 6 thêm transition method,
// không cần sửa enum:
// PENDING -> CONFIRMED -> PREPARING -> SHIPPING -> DELIVERED
//                       \-> CANCELLED
// DELIVERED -> RETURN_REQUESTED -> RETURNED
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PREPARING,
    SHIPPING,
    DELIVERED,
    CANCELLED,
    RETURN_REQUESTED,
    RETURNED
}
