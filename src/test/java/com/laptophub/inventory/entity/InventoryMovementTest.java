package com.laptophub.inventory.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryMovementTest {

    @Test
    void create_setsAllFields() {
        InventoryMovement movement = InventoryMovement.create(1L, InventoryMovementType.RECEIPT, 10, 10, 0,
                "STOCK_RECEIPT", 5L, null, 99L);

        assertThat(movement.getProductVariantId()).isEqualTo(1L);
        assertThat(movement.getType()).isEqualTo(InventoryMovementType.RECEIPT);
        assertThat(movement.getQuantity()).isEqualTo(10);
        assertThat(movement.getOnHandAfter()).isEqualTo(10);
        assertThat(movement.getReservedAfter()).isEqualTo(0);
        assertThat(movement.getReferenceType()).isEqualTo("STOCK_RECEIPT");
        assertThat(movement.getReferenceId()).isEqualTo(5L);
        assertThat(movement.getReason()).isNull();
        assertThat(movement.getCreatedByUserId()).isEqualTo(99L);
    }

    @Test
    void create_allowsNullReferenceAndReasonAndCreatedBy() {
        InventoryMovement movement = InventoryMovement.create(1L, InventoryMovementType.RESERVE, 2, 10, 2, null,
                null, null, null);

        assertThat(movement.getReferenceType()).isNull();
        assertThat(movement.getReferenceId()).isNull();
        assertThat(movement.getReason()).isNull();
        assertThat(movement.getCreatedByUserId()).isNull();
    }

    @Test
    void create_rejectsNullRequiredFields() {
        assertThatThrownBy(() -> InventoryMovement.create(null, InventoryMovementType.RECEIPT, 1, 1, 0, null, null,
                null, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(
                () -> InventoryMovement.create(1L, null, 1, 1, 0, null, null, null, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> InventoryMovement.create(1L, InventoryMovementType.RECEIPT, null, 1, 0, null, null,
                null, null)).isInstanceOf(NullPointerException.class);
    }
}
