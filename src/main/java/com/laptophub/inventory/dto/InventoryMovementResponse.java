package com.laptophub.inventory.dto;

import com.laptophub.inventory.entity.InventoryMovement;
import com.laptophub.inventory.entity.InventoryMovementType;

import java.time.Instant;

public record InventoryMovementResponse(
        Long id,
        Long productVariantId,
        InventoryMovementType type,
        Integer quantity,
        Integer onHandAfter,
        Integer reservedAfter,
        String referenceType,
        Long referenceId,
        String reason,
        Long createdByUserId,
        Instant createdAt) {

    public static InventoryMovementResponse from(InventoryMovement movement) {
        return new InventoryMovementResponse(
                movement.getId(),
                movement.getProductVariantId(),
                movement.getType(),
                movement.getQuantity(),
                movement.getOnHandAfter(),
                movement.getReservedAfter(),
                movement.getReferenceType(),
                movement.getReferenceId(),
                movement.getReason(),
                movement.getCreatedByUserId(),
                movement.getCreatedAt());
    }
}
