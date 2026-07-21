package com.laptophub.catalog.repository;

import com.laptophub.catalog.entity.Category;
import com.laptophub.catalog.entity.CategoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    boolean existsBySlug(String slug);

    List<Category> findByStatusOrderByNameAsc(CategoryStatus status);
}
