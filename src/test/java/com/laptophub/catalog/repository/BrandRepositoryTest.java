package com.laptophub.catalog.repository;

import com.laptophub.catalog.entity.Brand;
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

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class BrandRepositoryTest {

    @Autowired
    private BrandRepository brandRepository;

    @Test
    void findBySlug_returnsBrand_whenExists() {
        brandRepository.saveAndFlush(Brand.create("Asus", "asus-findbyslug", null, null));

        Optional<Brand> found = brandRepository.findBySlug("asus-findbyslug");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Asus");
    }

    @Test
    void findBySlug_returnsEmpty_whenMissing() {
        assertThat(brandRepository.findBySlug("khong-ton-tai")).isEmpty();
    }

    @Test
    void existsBySlug_reflectsSavedSlug() {
        brandRepository.saveAndFlush(Brand.create("Dell", "dell-existsbyslug", null, null));

        assertThat(brandRepository.existsBySlug("dell-existsbyslug")).isTrue();
        assertThat(brandRepository.existsBySlug("khong-ton-tai")).isFalse();
    }

    @Test
    void existsBySlugAndIdNot_excludesGivenId() {
        Brand saved = brandRepository.saveAndFlush(Brand.create("Lenovo", "lenovo-excludeid", null, null));

        assertThat(brandRepository.existsBySlugAndIdNot("lenovo-excludeid", saved.getId())).isFalse();
        assertThat(brandRepository.existsBySlugAndIdNot("lenovo-excludeid", saved.getId() + 1)).isTrue();
    }

    @Test
    void savingDuplicateSlug_violatesUniqueConstraint() {
        brandRepository.saveAndFlush(Brand.create("Asus", "asus-dup", null, null));
        Brand duplicate = Brand.create("Asus Khac", "asus-dup", null, null);

        assertThatThrownBy(() -> brandRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
