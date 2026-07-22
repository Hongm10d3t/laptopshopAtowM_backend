package com.laptophub.inventory.service;

import com.laptophub.catalog.entity.ProductVariant;
import com.laptophub.catalog.service.ProductVariantService;
import com.laptophub.common.ErrorCode;
import com.laptophub.common.exception.AppException;
import com.laptophub.inventory.entity.InventoryBalance;
import com.laptophub.inventory.entity.InventoryMovement;
import com.laptophub.inventory.entity.InventoryMovementType;
import com.laptophub.inventory.repository.InventoryBalanceRepository;
import com.laptophub.inventory.repository.InventoryMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    private static final Long VARIANT_ID = 1L;

    @Mock
    private InventoryBalanceRepository inventoryBalanceRepository;

    @Mock
    private InventoryMovementRepository inventoryMovementRepository;

    @Mock
    private ProductVariantService productVariantService;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(inventoryBalanceRepository, inventoryMovementRepository,
                productVariantService);
        lenient().when(productVariantService.getByIdOrThrow(VARIANT_ID))
                .thenReturn(ProductVariant.create(1L, "SKU-001", null, BigDecimal.TEN, null, null, null, null));
    }

    @Test
    void getBalance_throwsResourceNotFound_whenVariantMissing() {
        when(productVariantService.getByIdOrThrow(99L)).thenThrow(new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        assertThatThrownBy(() -> inventoryService.getBalance(99L)).isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void getBalance_returnsTransientZero_whenNoBalanceRowYet() {
        when(inventoryBalanceRepository.findByProductVariantId(VARIANT_ID)).thenReturn(Optional.empty());

        InventoryBalance balance = inventoryService.getBalance(VARIANT_ID);

        assertThat(balance.getOnHandQuantity()).isZero();
        assertThat(balance.getReservedQuantity()).isZero();
    }

    @Test
    void receiveStock_createsBalanceRow_appliesDelta_andRecordsReceiptMovement() {
        when(inventoryBalanceRepository.findByProductVariantId(VARIANT_ID)).thenReturn(Optional.empty());
        InventoryBalance created = InventoryBalance.create(VARIANT_ID);
        when(inventoryBalanceRepository.save(any(InventoryBalance.class))).thenReturn(created);
        when(inventoryBalanceRepository.applyOnHandDelta(VARIANT_ID, 10)).thenReturn(1);

        InventoryBalance after = withBalanceLookupSequence(created);

        InventoryBalance result = inventoryService.receiveStock(VARIANT_ID, 10, "STOCK_RECEIPT", 5L, 9L);

        assertThat(result).isSameAs(after);
        ArgumentCaptor<InventoryMovement> captor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(inventoryMovementRepository).save(captor.capture());
        InventoryMovement movement = captor.getValue();
        assertThat(movement.getType()).isEqualTo(InventoryMovementType.RECEIPT);
        assertThat(movement.getQuantity()).isEqualTo(10);
        assertThat(movement.getReferenceType()).isEqualTo("STOCK_RECEIPT");
        assertThat(movement.getReferenceId()).isEqualTo(5L);
        assertThat(movement.getCreatedByUserId()).isEqualTo(9L);
    }

    @Test
    void receiveStock_throwsInsufficientStock_whenConditionalUpdateAffectsZeroRows() {
        when(inventoryBalanceRepository.findByProductVariantId(VARIANT_ID))
                .thenReturn(Optional.of(InventoryBalance.create(VARIANT_ID)));
        when(inventoryBalanceRepository.applyOnHandDelta(VARIANT_ID, -5)).thenReturn(0);

        assertThatThrownBy(() -> inventoryService.receiveStock(VARIANT_ID, -5, null, null, null))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_STOCK));
        verify(inventoryMovementRepository, never()).save(any());
    }

    @Test
    void adjust_rejectsZeroDelta() {
        assertThatThrownBy(() -> inventoryService.adjust(VARIANT_ID, 0, "Kiem ke", 1L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void adjust_rejectsBlankReason() {
        assertThatThrownBy(() -> inventoryService.adjust(VARIANT_ID, 5, "  ", 1L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void adjust_positiveDelta_recordsAdjustmentIn() {
        InventoryBalance existing = InventoryBalance.create(VARIANT_ID);
        when(inventoryBalanceRepository.findByProductVariantId(VARIANT_ID)).thenReturn(Optional.of(existing));
        when(inventoryBalanceRepository.applyOnHandDelta(VARIANT_ID, 3)).thenReturn(1);

        inventoryService.adjust(VARIANT_ID, 3, "Kiem ke thua", 1L);

        ArgumentCaptor<InventoryMovement> captor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(inventoryMovementRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(InventoryMovementType.ADJUSTMENT_IN);
        assertThat(captor.getValue().getQuantity()).isEqualTo(3);
        assertThat(captor.getValue().getReason()).isEqualTo("Kiem ke thua");
    }

    @Test
    void adjust_negativeDelta_recordsAdjustmentOut() {
        InventoryBalance existing = InventoryBalance.create(VARIANT_ID);
        when(inventoryBalanceRepository.findByProductVariantId(VARIANT_ID)).thenReturn(Optional.of(existing));
        when(inventoryBalanceRepository.applyOnHandDelta(VARIANT_ID, -2)).thenReturn(1);

        inventoryService.adjust(VARIANT_ID, -2, "Hang hong", 1L);

        ArgumentCaptor<InventoryMovement> captor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(inventoryMovementRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(InventoryMovementType.ADJUSTMENT_OUT);
        assertThat(captor.getValue().getQuantity()).isEqualTo(2);
    }

    @Test
    void reserve_throwsInsufficientStock_whenNotEnoughAvailable() {
        when(inventoryBalanceRepository.reserveQuantity(VARIANT_ID, 5)).thenReturn(0);

        assertThatThrownBy(() -> inventoryService.reserve(VARIANT_ID, 5, "ORDER_ITEM", 10L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_STOCK));
    }

    @Test
    void reserve_recordsReserveMovement_whenSucceeds() {
        when(inventoryBalanceRepository.reserveQuantity(VARIANT_ID, 5)).thenReturn(1);
        when(inventoryBalanceRepository.findByProductVariantId(VARIANT_ID))
                .thenReturn(Optional.of(InventoryBalance.create(VARIANT_ID)));

        inventoryService.reserve(VARIANT_ID, 5, "ORDER_ITEM", 10L);

        ArgumentCaptor<InventoryMovement> captor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(inventoryMovementRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(InventoryMovementType.RESERVE);
        assertThat(captor.getValue().getReferenceType()).isEqualTo("ORDER_ITEM");
        assertThat(captor.getValue().getReferenceId()).isEqualTo(10L);
    }

    @Test
    void release_throwsResourceConflict_whenNotEnoughReserved() {
        when(inventoryBalanceRepository.releaseQuantity(VARIANT_ID, 5)).thenReturn(0);

        assertThatThrownBy(() -> inventoryService.release(VARIANT_ID, 5, "ORDER_ITEM", 10L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_CONFLICT));
    }

    @Test
    void fulfill_throwsInsufficientStock_whenNotEnough() {
        when(inventoryBalanceRepository.fulfillQuantity(VARIANT_ID, 5)).thenReturn(0);

        assertThatThrownBy(() -> inventoryService.fulfill(VARIANT_ID, 5, "ORDER_ITEM", 10L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_STOCK));
    }

    @Test
    void fulfill_recordsShipmentMovement_whenSucceeds() {
        when(inventoryBalanceRepository.fulfillQuantity(VARIANT_ID, 2)).thenReturn(1);
        when(inventoryBalanceRepository.findByProductVariantId(VARIANT_ID))
                .thenReturn(Optional.of(InventoryBalance.create(VARIANT_ID)));

        inventoryService.fulfill(VARIANT_ID, 2, "ORDER_ITEM", 10L);

        ArgumentCaptor<InventoryMovement> captor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(inventoryMovementRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(InventoryMovementType.SHIPMENT);
    }

    @Test
    void receiveReturn_recordsReturnMovement() {
        when(inventoryBalanceRepository.findByProductVariantId(VARIANT_ID))
                .thenReturn(Optional.of(InventoryBalance.create(VARIANT_ID)));
        when(inventoryBalanceRepository.applyOnHandDelta(VARIANT_ID, 1)).thenReturn(1);

        inventoryService.receiveReturn(VARIANT_ID, 1, "ORDER_ITEM", 10L);

        ArgumentCaptor<InventoryMovement> captor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(inventoryMovementRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(InventoryMovementType.RETURN);
    }

    private InventoryBalance withBalanceLookupSequence(InventoryBalance afterCreate) {
        when(inventoryBalanceRepository.findByProductVariantId(VARIANT_ID))
                .thenReturn(Optional.empty(), Optional.of(afterCreate));
        return afterCreate;
    }
}
