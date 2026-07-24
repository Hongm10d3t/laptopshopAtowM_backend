package com.laptophub.cart.repository;

import com.laptophub.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCartId(Long cartId);

    Optional<CartItem> findByCartIdAndProductVariantId(Long cartId, Long productVariantId);

    // Dùng để kiểm tra ownership khi Customer thao tác trên 1 dòng giỏ hàng cụ
    // thể — không tồn tại hoặc không thuộc cart này đều coi như not found.
    Optional<CartItem> findByIdAndCartId(Long id, Long cartId);

    // Dùng bởi OrderService.checkout sau khi tạo đơn thành công (xoá sạch giỏ).
    @Modifying(clearAutomatically = true)
    void deleteByCartId(Long cartId);
}
