package com.laptophub.order.repository;

import com.laptophub.order.entity.Order;
import com.laptophub.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUserId(Long userId, Pageable pageable);

    // Dùng để kiểm tra ownership khi Customer xem chi tiết 1 đơn cụ thể —
    // không tồn tại hoặc không phải của user này đều coi như not found, giống
    // AddressRepository.findByIdAndUserId.
    Optional<Order> findByIdAndUserId(Long id, Long userId);

    // Dùng cho Admin liệt kê đơn của mọi Customer, lọc theo trạng thái (tùy
    // chọn) — mirror StockReceiptRepository.search.
    @Query("SELECT o FROM Order o WHERE (:status IS NULL OR o.status = :status) ORDER BY o.createdAt DESC")
    Page<Order> search(@Param("status") OrderStatus status, Pageable pageable);
}
