package com.laptophub.inventory.service;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StockReceiptService {

    private final StockReceiptRepository stockReceiptRepository;
    private final StockReceiptItemRepository stockReceiptItemRepository;
    private final ProductVariantService productVariantService;
    private final InventoryService inventoryService;
    private final Clock clock;

    public StockReceiptService(StockReceiptRepository stockReceiptRepository,
            StockReceiptItemRepository stockReceiptItemRepository, ProductVariantService productVariantService,
            InventoryService inventoryService, Clock clock) {
        this.stockReceiptRepository = stockReceiptRepository;
        this.stockReceiptItemRepository = stockReceiptItemRepository;
        this.productVariantService = productVariantService;
        this.inventoryService = inventoryService;
        this.clock = clock;
    }

    @Transactional
    public StockReceipt create(StockReceiptCreateRequest request, Long createdByUserId) {
        if (stockReceiptRepository.existsByCode(request.code())) {
            throw new AppException(ErrorCode.RESOURCE_CONFLICT, "Mã phiếu nhập đã tồn tại");
        }
        validateItems(request.items());

        StockReceipt receipt = stockReceiptRepository
                .save(StockReceipt.create(request.code(), request.note(), createdByUserId));
        saveItems(receipt.getId(), request.items());
        return receipt;
    }

    @Transactional
    public StockReceipt replaceItems(Long receiptId, List<StockReceiptItemRequest> items) {
        StockReceipt receipt = getByIdOrThrow(receiptId);
        if (receipt.getStatus() != StockReceiptStatus.DRAFT) {
            throw new AppException(ErrorCode.INVALID_STOCK_RECEIPT_STATUS);
        }
        validateItems(items);

        stockReceiptItemRepository.deleteByStockReceiptId(receiptId);
        saveItems(receiptId, items);
        return receipt;
    }

    @Transactional
    public StockReceipt confirm(Long receiptId, Long confirmedByUserId) {
        StockReceipt receipt = getByIdOrThrow(receiptId);
        List<StockReceiptItem> items = stockReceiptItemRepository.findByStockReceiptId(receiptId);
        if (items.isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Phiếu nhập chưa có dòng hàng nào");
        }

        for (StockReceiptItem item : items) {
            inventoryService.receiveStock(item.getProductVariantId(), item.getQuantity(), "STOCK_RECEIPT", receiptId,
                    confirmedByUserId);
        }
        receipt.confirm(confirmedByUserId, clock.instant());
        return receipt;
    }

    @Transactional
    public StockReceipt cancel(Long receiptId, Long cancelledByUserId) {
        StockReceipt receipt = getByIdOrThrow(receiptId);
        receipt.cancel(cancelledByUserId, clock.instant());
        return receipt;
    }

    public StockReceipt getByIdOrThrow(Long receiptId) {
        return stockReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    public List<StockReceiptItem> getItems(Long receiptId) {
        return stockReceiptItemRepository.findByStockReceiptId(receiptId);
    }

    public Page<StockReceipt> list(StockReceiptStatus status, Pageable pageable) {
        return stockReceiptRepository.search(status, pageable);
    }

    private void validateItems(List<StockReceiptItemRequest> items) {
        long distinctVariantCount = items.stream().map(StockReceiptItemRequest::productVariantId).distinct().count();
        if (distinctVariantCount != items.size()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Phiếu nhập không được có 2 dòng cùng 1 biến thể");
        }
        items.forEach(item -> productVariantService.getByIdOrThrow(item.productVariantId()));
    }

    private void saveItems(Long receiptId, List<StockReceiptItemRequest> items) {
        List<StockReceiptItem> entities = items.stream()
                .map(item -> StockReceiptItem.create(receiptId, item.productVariantId(), item.quantity()))
                .collect(Collectors.toList());
        stockReceiptItemRepository.saveAll(entities);
    }
}
