package com.laptophub.cart.repository;

import com.laptophub.cart.entity.Cart;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUserId(Long userId);

    // Chỉ dùng ở OrderService.checkout: khoá pessimistic ngay đầu transaction để
    // serialize các request checkout đồng thời của cùng 1 user (double-click,
    // network retry) — tránh 2 request cùng đọc chung giỏ hàng rồi tạo 2 đơn.
    // Không dùng cho các thao tác đọc/sửa giỏ hàng thông thường ở CartService.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cart c WHERE c.userId = :userId")
    Optional<Cart> findByUserIdForUpdate(@Param("userId") Long userId);
}
