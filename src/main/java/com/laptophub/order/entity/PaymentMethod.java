package com.laptophub.order.entity;

// Chỉ COD thực sự dùng được ở Giai đoạn 5 (Order.create luôn gán COD).
// ONLINE để sẵn giá trị cho Giai đoạn 7 khi tích hợp cổng thanh toán —
// tránh phải migration thêm giá trị enum sau.
public enum PaymentMethod {
    COD,
    ONLINE
}
