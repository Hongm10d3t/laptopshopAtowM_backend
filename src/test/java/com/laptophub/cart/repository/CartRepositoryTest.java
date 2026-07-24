package com.laptophub.cart.repository;

import com.laptophub.cart.entity.Cart;
import com.laptophub.config.JpaAuditingConfig;
import com.laptophub.user.EmailNormalizer;
import com.laptophub.user.entity.User;
import com.laptophub.user.entity.UserRole;
import com.laptophub.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class CartRepositoryTest {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private UserRepository userRepository;

    private Long newUserId(String email) {
        User user = userRepository.saveAndFlush(
                User.create(EmailNormalizer.normalize(email), "hash", "Name", null, UserRole.CUSTOMER));
        return user.getId();
    }

    @Test
    void findByUserId_returnsEmpty_whenNoCartYet() {
        Long userId = newUserId("no-cart@example.com");

        assertThat(cartRepository.findByUserId(userId)).isEmpty();
    }

    @Test
    void findByUserId_returnsCart_afterSaved() {
        Long userId = newUserId("has-cart@example.com");
        Cart saved = cartRepository.saveAndFlush(Cart.create(userId));

        Optional<Cart> found = cartRepository.findByUserId(userId);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    void findByUserIdForUpdate_returnsCart_sameAsFindByUserId() {
        Long userId = newUserId("lock@example.com");
        Cart saved = cartRepository.saveAndFlush(Cart.create(userId));

        Optional<Cart> found = cartRepository.findByUserIdForUpdate(userId);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
    }
}
