package com.laptophub.order.entity;

import com.laptophub.common.ErrorCode;
import com.laptophub.common.entity.BaseEntity;
import com.laptophub.common.exception.AppException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

// userId là FK dạng Long phẳng, đúng tiền lệ chung của dự án. recipientName..
// streetAddress là snapshot địa chỉ giao hàng tại thời điểm đặt đơn (không FK
// tới Address) — địa chỉ gốc có thể bị Customer sửa/xoá sau đó.
// paymentMethod luôn COD ở Giai đoạn 5 (chưa có luồng ONLINE) — hardcode
// trong constructor thay vì nhận tham số, tránh API cho phép giá trị chưa
// triển khai được.
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "recipient_name", nullable = false, length = 255)
    private String recipientName;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "province", nullable = false, length = 255)
    private String province;

    @Column(name = "district", nullable = false, length = 255)
    private String district;

    @Column(name = "ward", nullable = false, length = 255)
    private String ward;

    @Column(name = "street_address", nullable = false, length = 500)
    private String streetAddress;

    private Order(Long userId, BigDecimal totalAmount, String note, String recipientName, String phone,
                   String province, String district, String ward, String streetAddress) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.status = OrderStatus.PENDING;
        this.paymentMethod = PaymentMethod.COD;
        this.totalAmount = Objects.requireNonNull(totalAmount, "totalAmount must not be null");
        this.note = note;
        this.recipientName = Objects.requireNonNull(recipientName, "recipientName must not be null");
        this.phone = Objects.requireNonNull(phone, "phone must not be null");
        this.province = Objects.requireNonNull(province, "province must not be null");
        this.district = Objects.requireNonNull(district, "district must not be null");
        this.ward = Objects.requireNonNull(ward, "ward must not be null");
        this.streetAddress = Objects.requireNonNull(streetAddress, "streetAddress must not be null");
    }

    public static Order create(Long userId, BigDecimal totalAmount, String note, String recipientName, String phone,
                                String province, String district, String ward, String streetAddress) {
        return new Order(userId, totalAmount, note, recipientName, phone, province, district, ward, streetAddress);
    }

    // Tập trạng thái được phép hủy là tập RỘNG NHẤT (năng lực Admin) —
    // OrderService.cancelByCustomer tự kiểm tra phạm vi hẹp hơn (chỉ
    // PENDING/CONFIRMED) TRƯỚC khi gọi cancel(), entity không phân biệt ai gọi.
    private static final Set<OrderStatus> CANCELLABLE_STATUSES =
            EnumSet.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PREPARING);

    public void confirm() {
        requireStatus(OrderStatus.PENDING);
        this.status = OrderStatus.CONFIRMED;
    }

    public void prepare() {
        requireStatus(OrderStatus.CONFIRMED);
        this.status = OrderStatus.PREPARING;
    }

    public void ship() {
        requireStatus(OrderStatus.PREPARING);
        this.status = OrderStatus.SHIPPING;
    }

    public void deliver() {
        requireStatus(OrderStatus.SHIPPING);
        this.status = OrderStatus.DELIVERED;
    }

    public void cancel() {
        if (!CANCELLABLE_STATUSES.contains(this.status)) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }
        this.status = OrderStatus.CANCELLED;
    }

    public void requestReturn() {
        requireStatus(OrderStatus.DELIVERED);
        this.status = OrderStatus.RETURN_REQUESTED;
    }

    public void approveReturn() {
        requireStatus(OrderStatus.RETURN_REQUESTED);
        this.status = OrderStatus.RETURNED;
    }

    public void rejectReturn() {
        requireStatus(OrderStatus.RETURN_REQUESTED);
        this.status = OrderStatus.DELIVERED;
    }

    private void requireStatus(OrderStatus expected) {
        if (this.status != expected) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }
    }
}
