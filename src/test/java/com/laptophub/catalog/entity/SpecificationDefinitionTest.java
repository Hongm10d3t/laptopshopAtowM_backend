package com.laptophub.catalog.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpecificationDefinitionTest {

    @Test
    void create_setsFields_categoryIdNullable() {
        SpecificationDefinition spec = SpecificationDefinition.create(null, "cpu", "Bo xu ly", null, "Hieu nang", 10);

        assertThat(spec.getCategoryId()).isNull();
        assertThat(spec.getCode()).isEqualTo("cpu");
        assertThat(spec.getLabel()).isEqualTo("Bo xu ly");
        assertThat(spec.getUnit()).isNull();
        assertThat(spec.getGroupLabel()).isEqualTo("Hieu nang");
        assertThat(spec.getDisplayOrder()).isEqualTo(10);
    }

    @Test
    void create_acceptsCategorySpecificSpec() {
        SpecificationDefinition spec = SpecificationDefinition.create(5L, "screen_size", "Kich thuoc", "inch", null, 1);

        assertThat(spec.getCategoryId()).isEqualTo(5L);
        assertThat(spec.getUnit()).isEqualTo("inch");
    }

    @Test
    void create_rejectsNullCode() {
        assertThatThrownBy(() -> SpecificationDefinition.create(null, null, "Label", null, null, 0))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void create_rejectsNullLabel() {
        assertThatThrownBy(() -> SpecificationDefinition.create(null, "cpu", null, null, null, 0))
                .isInstanceOf(NullPointerException.class);
    }
}
