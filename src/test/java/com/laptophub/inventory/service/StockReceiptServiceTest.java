package com.laptophub.inventory.service;

import com.laptophub.catalog.entity.ProductVariant;
import com.laptophub.catalog.service.ProductVariantService;
import com.laptophub.common.ErrorCode;
import com.laptophub.common.exception.AppException;
import com.laptophub.inventory.dto.StockReceiptCreateRequest;
import com.laptophub.inventory.dto.StockReceiptItemRequest;
import com.laptophub.inventory.entity.StockReceipt;
import com.laptophub.inventory.entity.StockReceiptItem;
import com.laptophub.inventory.entity.StockReceiptStatus;
import com.laptophub.inventory.repository.StockReceiptItemRepository;
import com.laptophub.inventory.repository.StockReceiptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockReceiptServiceTest {

    @Mock
    private StockReceiptRepository stockReceiptRepository;

    @Mock
    private StockReceiptItemRepository stockReceiptItemRepository;

    @Mock
    private ProductVariantService productVariantService;

    @Mock
    private InventoryService inventoryService;

    private StockReceiptService stockReceiptService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-07-22T00:00:00Z"), ZoneOffset.UTC);
        stockReceiptService = new StockReceiptService(stockReceiptRepository, stockReceiptItemRepository,
                productVariantService, inventoryService, fixedClock);
    }

    private StockReceiptCreateRequest createRequest(StockReceiptItemRequest... items) {
        return new StockReceiptCreateRequest("PN-001", "Nhap tu NCC A", List.of(items));
    }

    @Test
    void create_throwsResourceConflict_whenCodeAlreadyExists() {
        when(stockReceiptRepository.existsByCode("PN-001")).thenReturn(true);

        assertThatThrownBy(() -> stockReceiptService.create(createRequest(new StockReceiptItemRequest(1L, 5)), 1L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_CONFLICT));
    }

    @Test
    void create_throwsValidationError_whenDuplicateVariantInItems() {
        when(stockReceiptRepository.existsByCode("PN-001")).thenReturn(false);

        StockReceiptCreateRequest request = createRequest(new StockReceiptItemRequest(1L, 5),
                new StockReceiptItemRequest(1L, 3));

        assertThatThrownBy(() -> stockReceiptService.create(request, 1L)).isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void create_propagatesResourceNotFound_whenVariantMissing() {
        when(stockReceiptRepository.existsByCode("PN-001")).thenReturn(false);
        when(productVariantService.getByIdOrThrow(99L)).thenThrow(new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        assertThatThrownBy(() -> stockReceiptService.create(createRequest(new StockReceiptItemRequest(99L, 5)), 1L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void create_savesReceiptAndItems_whenValid() {
        when(stockReceiptRepository.existsByCode("PN-001")).thenReturn(false);
        when(productVariantService.getByIdOrThrow(1L))
                .thenReturn(ProductVariant.create(1L, "SKU-001", null, BigDecimal.TEN, null, null, null, null));
        StockReceipt saved = StockReceipt.create("PN-001", "Nhap tu NCC A", 1L);
        saved.setId(1L);
        when(stockReceiptRepository.save(any(StockReceipt.class))).thenReturn(saved);

        StockReceipt result = stockReceiptService.create(createRequest(new StockReceiptItemRequest(1L, 5)), 1L);

        assertThat(result.getCode()).isEqualTo("PN-001");
        verify(stockReceiptItemRepository).saveAll(any());
    }

    @Test
    void replaceItems_throwsInvalidStatus_whenNotDraft() {
        StockReceipt confirmed = StockReceipt.create("PN-002", null, 1L);
        confirmed.confirm(1L, Instant.now());
        when(stockReceiptRepository.findById(2L)).thenReturn(Optional.of(confirmed));

        assertThatThrownBy(() -> stockReceiptService.replaceItems(2L, List.of(new StockReceiptItemRequest(1L, 5))))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_STOCK_RECEIPT_STATUS));
        verify(stockReceiptItemRepository, never()).deleteByStockReceiptId(any());
    }

    @Test
    void replaceItems_deletesThenSaves_whenDraft() {
        StockReceipt draft = StockReceipt.create("PN-003", null, 1L);
        when(stockReceiptRepository.findById(3L)).thenReturn(Optional.of(draft));
        when(productVariantService.getByIdOrThrow(1L))
                .thenReturn(ProductVariant.create(1L, "SKU-001", null, BigDecimal.TEN, null, null, null, null));

        stockReceiptService.replaceItems(3L, List.of(new StockReceiptItemRequest(1L, 5)));

        verify(stockReceiptItemRepository).deleteByStockReceiptId(3L);
        verify(stockReceiptItemRepository).saveAll(any());
    }

    @Test
    void confirm_throwsResourceNotFound_whenReceiptMissing() {
        when(stockReceiptRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockReceiptService.confirm(404L, 1L)).isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void confirm_throwsValidationError_whenNoItems() {
        StockReceipt draft = StockReceipt.create("PN-004", null, 1L);
        when(stockReceiptRepository.findById(4L)).thenReturn(Optional.of(draft));
        when(stockReceiptItemRepository.findByStockReceiptId(4L)).thenReturn(List.of());

        assertThatThrownBy(() -> stockReceiptService.confirm(4L, 1L)).isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
        verify(inventoryService, never()).receiveStock(anyLong(), anyInt(), any(), any(), any());
    }

    @Test
    void confirm_callsReceiveStockForEveryItem_thenTransitionsToConfirmed() {
        StockReceipt draft = StockReceipt.create("PN-005", null, 1L);
        when(stockReceiptRepository.findById(5L)).thenReturn(Optional.of(draft));
        when(stockReceiptItemRepository.findByStockReceiptId(5L)).thenReturn(List.of(
                StockReceiptItem.create(5L, 1L, 10),
                StockReceiptItem.create(5L, 2L, 4)));

        StockReceipt result = stockReceiptService.confirm(5L, 9L);

        assertThat(result.getStatus()).isEqualTo(StockReceiptStatus.CONFIRMED);
        assertThat(result.getConfirmedByUserId()).isEqualTo(9L);
        verify(inventoryService, times(1)).receiveStock(1L, 10, "STOCK_RECEIPT", 5L, 9L);
        verify(inventoryService, times(1)).receiveStock(2L, 4, "STOCK_RECEIPT", 5L, 9L);
    }

    @Test
    void confirm_whenAlreadyConfirmed_throwsInvalidStatus() {
        StockReceipt confirmed = StockReceipt.create("PN-006", null, 1L);
        confirmed.confirm(1L, Instant.now());
        when(stockReceiptRepository.findById(6L)).thenReturn(Optional.of(confirmed));
        when(stockReceiptItemRepository.findByStockReceiptId(6L))
                .thenReturn(List.of(StockReceiptItem.create(6L, 1L, 5)));

        assertThatThrownBy(() -> stockReceiptService.confirm(6L, 1L)).isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_STOCK_RECEIPT_STATUS));
    }

    @Test
    void cancel_transitionsDraftToCancelled() {
        StockReceipt draft = StockReceipt.create("PN-007", null, 1L);
        when(stockReceiptRepository.findById(7L)).thenReturn(Optional.of(draft));

        StockReceipt result = stockReceiptService.cancel(7L, 2L);

        assertThat(result.getStatus()).isEqualTo(StockReceiptStatus.CANCELLED);
        assertThat(result.getCancelledByUserId()).isEqualTo(2L);
    }

    @Test
    void cancel_whenAlreadyConfirmed_throwsInvalidStatus() {
        StockReceipt confirmed = StockReceipt.create("PN-008", null, 1L);
        confirmed.confirm(1L, Instant.now());
        when(stockReceiptRepository.findById(8L)).thenReturn(Optional.of(confirmed));

        assertThatThrownBy(() -> stockReceiptService.cancel(8L, 2L)).isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_STOCK_RECEIPT_STATUS));
    }
}
