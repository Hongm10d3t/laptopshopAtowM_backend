package com.laptophub.user.repository;

import com.laptophub.user.entity.User;
import com.laptophub.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(UserRole role);
}
