package com.laptophub.inventory.repository;

import com.laptophub.config.JpaAuditingConfig;
import com.laptophub.inventory.entity.StockReceipt;
import com.laptophub.inventory.entity.StockReceiptStatus;
import com.laptophub.user.EmailNormalizer;
import com.laptophub.user.entity.User;
import com.laptophub.user.entity.UserRole;
import com.laptophub.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class StockReceiptRepositoryTest {

    @Autowired
    private StockReceiptRepository stockReceiptRepository;

    @Autowired
    private UserRepository userRepository;

    private Long newUserId(String email) {
        return userRepository
                .saveAndFlush(User.create(EmailNormalizer.normalize(email), "hash", "Admin", null, UserRole.ADMIN))
                .getId();
    }

    @Test
    void existsByCode_detectsDuplicate() {
        Long userId = newUserId("receipt-repo-exists@example.com");
        stockReceiptRepository.saveAndFlush(StockReceipt.create("PN-EXISTS-1", null, userId));

        assertThat(stockReceiptRepository.existsByCode("PN-EXISTS-1")).isTrue();
        assertThat(stockReceiptRepository.existsByCode("PN-EXISTS-2")).isFalse();
    }

    @Test
    void savingDuplicateCode_violatesUniqueConstraint() {
        Long userId = newUserId("receipt-repo-dup@example.com");
        stockReceiptRepository.saveAndFlush(StockReceipt.create("PN-DUP-1", null, userId));

        assertThatThrownBy(
                () -> stockReceiptRepository.saveAndFlush(StockReceipt.create("PN-DUP-1", null, userId)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void search_filtersByStatus_whenProvided() {
        Long userId = newUserId("receipt-repo-search@example.com");
        StockReceipt draft = stockReceiptRepository.saveAndFlush(StockReceipt.create("PN-SEARCH-DRAFT", null, userId));
        StockReceipt confirmed = StockReceipt.create("PN-SEARCH-CONFIRMED", null, userId);
        confirmed.confirm(userId, Instant.now());
        stockReceiptRepository.saveAndFlush(confirmed);

        Page<StockReceipt> draftsOnly = stockReceiptRepository.search(StockReceiptStatus.DRAFT, PageRequest.of(0, 10));
        Page<StockReceipt> all = stockReceiptRepository.search(null, PageRequest.of(0, 10));

        assertThat(draftsOnly.getContent()).extracting(StockReceipt::getId).contains(draft.getId())
                .doesNotContain(confirmed.getId());
        assertThat(all.getContent()).extracting(StockReceipt::getId).contains(draft.getId(), confirmed.getId());
    }
}
