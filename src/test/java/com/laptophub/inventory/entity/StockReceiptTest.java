package com.laptophub.inventory.entity;

import com.laptophub.common.ErrorCode;
import com.laptophub.common.exception.AppException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockReceiptTest {

    @Test
    void create_setsDraftStatus() {
        StockReceipt receipt = StockReceipt.create("PN-001", "Nhap tu NCC A", 1L);

        assertThat(receipt.getCode()).isEqualTo("PN-001");
        assertThat(receipt.getNote()).isEqualTo("Nhap tu NCC A");
        assertThat(receipt.getCreatedByUserId()).isEqualTo(1L);
        assertThat(receipt.getStatus()).isEqualTo(StockReceiptStatus.DRAFT);
        assertThat(receipt.getConfirmedAt()).isNull();
        assertThat(receipt.getCancelledAt()).isNull();
    }

    @Test
    void create_rejectsNullCodeOrCreatedBy() {
        assertThatThrownBy(() -> StockReceipt.create(null, null, 1L)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> StockReceipt.create("PN-001", null, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void confirm_fromDraft_transitionsToConfirmed() {
        StockReceipt receipt = StockReceipt.create("PN-002", null, 1L);
        Instant now = Instant.now();

        receipt.confirm(2L, now);

        assertThat(receipt.getStatus()).isEqualTo(StockReceiptStatus.CONFIRMED);
        assertThat(receipt.getConfirmedByUserId()).isEqualTo(2L);
        assertThat(receipt.getConfirmedAt()).isEqualTo(now);
    }

    @Test
    void confirm_whenNotDraft_throwsInvalidStatus() {
        StockReceipt receipt = StockReceipt.create("PN-003", null, 1L);
        receipt.confirm(2L, Instant.now());

        assertThatThrownBy(() -> receipt.confirm(2L, Instant.now())).isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_STOCK_RECEIPT_STATUS));
    }

    @Test
    void cancel_fromDraft_transitionsToCancelled() {
        StockReceipt receipt = StockReceipt.create("PN-004", null, 1L);
        Instant now = Instant.now();

        receipt.cancel(3L, now);

        assertThat(receipt.getStatus()).isEqualTo(StockReceiptStatus.CANCELLED);
        assertThat(receipt.getCancelledByUserId()).isEqualTo(3L);
        assertThat(receipt.getCancelledAt()).isEqualTo(now);
    }

    @Test
    void cancel_whenAlreadyConfirmed_throwsInvalidStatus() {
        StockReceipt receipt = StockReceipt.create("PN-005", null, 1L);
        receipt.confirm(2L, Instant.now());

        assertThatThrownBy(() -> receipt.cancel(3L, Instant.now())).isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_STOCK_RECEIPT_STATUS));
    }

    @Test
    void cancel_whenAlreadyCancelled_throwsInvalidStatus() {
        StockReceipt receipt = StockReceipt.create("PN-006", null, 1L);
        receipt.cancel(3L, Instant.now());

        assertThatThrownBy(() -> receipt.cancel(3L, Instant.now())).isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_STOCK_RECEIPT_STATUS));
    }
}
