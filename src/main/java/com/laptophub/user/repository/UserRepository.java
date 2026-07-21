package com.laptophub.user.repository;

import com.laptophub.user.entity.User;
import com.laptophub.user.entity.UserRole;
import com.laptophub.user.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(UserRole role);

    // Dùng cho Admin liệt kê/tìm kiếm tài khoản — mọi filter đều optional,
    // theo đúng pattern "(:param IS NULL OR ...)" đã dùng trong dự án.
    @Query("""
            select u from User u
            where (:role is null or u.role = :role)
              and (:status is null or u.status = :status)
              and (:keyword is null
                   or lower(u.email) like concat('%', :keyword, '%')
                   or lower(u.fullName) like concat('%', :keyword, '%'))
            """)
    Page<User> search(UserRole role, UserStatus status, String keyword, Pageable pageable);
}
