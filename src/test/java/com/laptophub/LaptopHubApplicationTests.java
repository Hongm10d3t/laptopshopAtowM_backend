package com.laptophub;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Trước đây test này loại trừ DataSource/Hibernate/Flyway + profile "test" để
// chạy không cần MySQL sống. Từ khi có bean thật cần UserRepository
// (com.laptophub.user.UserService, dùng bởi CustomUserDetailsService), context
// đầy đủ của app bắt buộc phải có DataSource/JPA — không thể loại trừ nữa, nên
// test chuyển sang tải context thật, giống mọi test khác trong project (đã
// yêu cầu MySQL local từ UserRepositoryTest).
@SpringBootTest
class LaptopHubApplicationTests {

    @Test
    void contextLoads() {
    }
}
