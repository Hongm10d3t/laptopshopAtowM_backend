package com.laptophub.order.repository;

import com.laptophub.config.JpaAuditingConfig;
import com.laptophub.order.entity.Order;
import com.laptophub.order.entity.OrderStatus;
import com.laptophub.order.entity.OrderStatusHistory;
import com.laptophub.user.EmailNormalizer;
import com.laptophub.user.entity.User;
import com.laptophub.user.entity.UserRole;
import com.laptophub.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class OrderStatusHistoryRepositoryTest {

    @Autowired
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    private Long newUserId(String email) {
        return userRepository.saveAndFlush(
                User.create(EmailNormalizer.normalize(email), "hash", "Name", null, UserRole.CUSTOMER)).getId();
    }

    private Long newOrderId(Long userId) {
        Order order = Order.create(userId, BigDecimal.TEN, null, "A", "0900000000", "HN", "CG", "DV", "123");
        return orderRepository.saveAndFlush(order).getId();
    }

    @Test
    void findByOrderIdOrderByCreatedAtAsc_returnsOldestFirst() {
        Long userId = newUserId("history-order@example.com");
        Long orderId = newOrderId(userId);
        orderStatusHistoryRepository.saveAndFlush(
                OrderStatusHistory.create(orderId, OrderStatus.PENDING, OrderStatus.CONFIRMED, userId, null));
        orderStatusHistoryRepository.saveAndFlush(
                OrderStatusHistory.create(orderId, OrderStatus.CONFIRMED, OrderStatus.PREPARING, userId, null));

        List<OrderStatusHistory> history = orderStatusHistoryRepository.findByOrderIdOrderByCreatedAtAsc(orderId);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).getToStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(history.get(1).getToStatus()).isEqualTo(OrderStatus.PREPARING);
    }

    @Test
    void findByOrderIdOrderByCreatedAtAsc_returnsEmpty_whenNoHistoryYet() {
        Long userId = newUserId("history-empty@example.com");
        Long orderId = newOrderId(userId);

        assertThat(orderStatusHistoryRepository.findByOrderIdOrderByCreatedAtAsc(orderId)).isEmpty();
    }
}
