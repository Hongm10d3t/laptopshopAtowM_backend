package com.laptophub.cart.entity;

import com.laptophub.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

// userId là FK dạng Long phẳng (không @ManyToOne), đúng tiền lệ chung của dự
// án (Address.userId, ProductVariant.productId...). 1 user chỉ có 1 Cart
// (UNIQUE ở migration) — được tạo lười ở lần thêm sản phẩm đầu tiên.
@Entity
@Table(name = "carts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    private Cart(Long userId) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
    }

    public static Cart create(Long userId) {
        return new Cart(userId);
    }
}
