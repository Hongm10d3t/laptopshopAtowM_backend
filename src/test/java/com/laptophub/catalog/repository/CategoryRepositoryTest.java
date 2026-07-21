package com.laptophub.catalog.repository;

import com.laptophub.catalog.entity.Category;
import com.laptophub.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Chạy trên MySQL thật (Replace.NONE), giống UserRepositoryTest/
// AddressRepositoryTest — project chưa có embedded test DB và migration
// dùng cú pháp riêng của MySQL.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void findBySlug_returnsCategory_whenExists() {
        categoryRepository.saveAndFlush(Category.create("Laptop", "laptop-findbyslug", null));

        Optional<Category> found = categoryRepository.findBySlug("laptop-findbyslug");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Laptop");
    }

    @Test
    void findBySlug_returnsEmpty_whenMissing() {
        assertThat(categoryRepository.findBySlug("khong-ton-tai")).isEmpty();
    }

    @Test
    void existsBySlug_reflectsSavedSlug() {
        categoryRepository.saveAndFlush(Category.create("Chuot", "chuot-existsbyslug", null));

        assertThat(categoryRepository.existsBySlug("chuot-existsbyslug")).isTrue();
        assertThat(categoryRepository.existsBySlug("khong-ton-tai")).isFalse();
    }

    @Test
    void existsBySlugAndIdNot_excludesGivenId() {
        Category saved = categoryRepository.saveAndFlush(Category.create("Ban phim", "ban-phim-excludeid", null));

        assertThat(categoryRepository.existsBySlugAndIdNot("ban-phim-excludeid", saved.getId())).isFalse();
        assertThat(categoryRepository.existsBySlugAndIdNot("ban-phim-excludeid", saved.getId() + 1)).isTrue();
    }

    @Test
    void savingDuplicateSlug_violatesUniqueConstraint() {
        categoryRepository.saveAndFlush(Category.create("Laptop", "laptop-dup", null));
        Category duplicate = Category.create("Laptop Khac", "laptop-dup", null);

        assertThatThrownBy(() -> categoryRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
