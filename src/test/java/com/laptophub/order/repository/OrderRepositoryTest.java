package com.laptophub.order.repository;

import com.laptophub.config.JpaAuditingConfig;
import com.laptophub.order.entity.Order;
import com.laptophub.user.EmailNormalizer;
import com.laptophub.user.entity.User;
import com.laptophub.user.entity.UserRole;
import com.laptophub.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    private Long newUserId(String email) {
        User user = userRepository.saveAndFlush(
                User.create(EmailNormalizer.normalize(email), "hash", "Name", null, UserRole.CUSTOMER));
        return user.getId();
    }

    private Order newOrder(Long userId) {
        return Order.create(userId, BigDecimal.TEN, null, "A", "0900000000", "HN", "CG", "DV", "123");
    }

    @Test
    void findByUserId_returnsOnlyOrdersOfThatUser() {
        Long ownerId = newUserId("order-owner@example.com");
        Long strangerId = newUserId("order-stranger@example.com");
        orderRepository.saveAndFlush(newOrder(ownerId));
        orderRepository.saveAndFlush(newOrder(strangerId));

        Page<Order> found = orderRepository.findByUserId(ownerId, PageRequest.of(0, 10));

        assertThat(found.getTotalElements()).isEqualTo(1);
        assertThat(found.getContent().get(0).getUserId()).isEqualTo(ownerId);
    }

    @Test
    void findByIdAndUserId_returnsEmpty_whenOrderBelongsToAnotherUser() {
        Long ownerId = newUserId("order-find-owner@example.com");
        Long strangerId = newUserId("order-find-stranger@example.com");
        Order order = orderRepository.saveAndFlush(newOrder(ownerId));

        Optional<Order> asOwner = orderRepository.findByIdAndUserId(order.getId(), ownerId);
        Optional<Order> asStranger = orderRepository.findByIdAndUserId(order.getId(), strangerId);

        assertThat(asOwner).isPresent();
        assertThat(asStranger).isEmpty();
    }
}
