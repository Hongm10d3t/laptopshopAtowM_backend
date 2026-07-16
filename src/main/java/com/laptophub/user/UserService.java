package com.laptophub.user;

import org.springframework.stereotype.Service;

import java.util.Optional;

// Cổng đọc tối thiểu cho module khác (vd security) — không được tự query
// UserRepository trực tiếp, phải đi qua đây (ranh giới đã chốt ở
// AUTH_SECURITY_USER_CONTRACT.md, gói ASU-00).
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Caller (vd CustomUserDetailsService) phải tự normalize qua EmailNormalizer
    // trước khi gọi — method này không normalize giúp.
    public Optional<User> findByNormalizedEmail(String normalizedEmail) {
        return userRepository.findByEmail(normalizedEmail);
    }
}
