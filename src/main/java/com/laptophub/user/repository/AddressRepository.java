package com.laptophub.user.repository;

import com.laptophub.user.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByUserIdOrderByIsDefaultDescCreatedAtDesc(Long userId);

    // Dùng để kiểm tra ownership khi Customer thao tác trên 1 địa chỉ cụ thể —
    // không tồn tại hoặc không phải của user này đều coi như not found, không
    // phân biệt 2 trường hợp để tránh lộ thông tin địa chỉ của người khác.
    Optional<Address> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    Optional<Address> findByUserIdAndIsDefaultTrue(Long userId);
}
