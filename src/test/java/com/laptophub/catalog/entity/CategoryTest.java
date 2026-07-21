package com.laptophub.catalog.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryTest {

    @Test
    void create_setsFieldsAndDefaultsToActive() {
        Category category = Category.create("Laptop", "laptop", "Laptop các loại");

        assertThat(category.getName()).isEqualTo("Laptop");
        assertThat(category.getSlug()).isEqualTo("laptop");
        assertThat(category.getDescription()).isEqualTo("Laptop các loại");
        assertThat(category.getStatus()).isEqualTo(CategoryStatus.ACTIVE);
    }

    @Test
    void create_rejectsNullName() {
        assertThatThrownBy(() -> Category.create(null, "laptop", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void create_rejectsNullSlug() {
        assertThatThrownBy(() -> Category.create("Laptop", null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void update_replacesNameSlugAndDescription() {
        Category category = Category.create("Laptop", "laptop", null);

        category.update("Laptop Gaming", "laptop-gaming", "Laptop chơi game");

        assertThat(category.getName()).isEqualTo("Laptop Gaming");
        assertThat(category.getSlug()).isEqualTo("laptop-gaming");
        assertThat(category.getDescription()).isEqualTo("Laptop chơi game");
    }

    @Test
    void deactivate_and_activate_toggleStatus() {
        Category category = Category.create("Laptop", "laptop", null);

        category.deactivate();
        assertThat(category.getStatus()).isEqualTo(CategoryStatus.INACTIVE);

        category.activate();
        assertThat(category.getStatus()).isEqualTo(CategoryStatus.ACTIVE);
    }
}
