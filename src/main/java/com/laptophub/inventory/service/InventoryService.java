package com.laptophub.inventory.service;

import com.laptophub.catalog.service.ProductVariantService;
import com.laptophub.common.ErrorCode;
import com.laptophub.common.exception.AppException;
import com.laptophub.inventory.entity.InventoryBalance;
import com.laptophub.inventory.entity.InventoryMovement;
import com.laptophub.inventory.entity.InventoryMovementType;
import com.laptophub.inventory.repository.InventoryBalanceRepository;
import com.laptophub.inventory.repository.InventoryMovementRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// reserve/release/fulfill/receiveReturn chưa có endpoint HTTP ở Giai đoạn 4 —
// Order module (Giai đoạn 5/6) sẽ gọi trực tiếp qua bean này khi OrderItem
// tồn tại, không qua HTTP (module giao tiếp qua service, PROJECT_RULES §2).
@Service
public class InventoryService {

    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final ProductVariantService productVariantService;

    public InventoryService(InventoryBalanceRepository inventoryBalanceRepository,
            InventoryMovementRepository inventoryMovementRepository, ProductVariantService productVariantService) {
        this.inventoryBalanceRepository = inventoryBalanceRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.productVariantService = productVariantService;
    }

    public InventoryBalance getBalance(Long productVariantId) {
        productVariantService.getByIdOrThrow(productVariantId);
        return inventoryBalanceRepository.findByProductVariantId(productVariantId)
                .orElseGet(() -> InventoryBalance.create(productVariantId));
    }

    public Page<InventoryMovement> listMovements(Long productVariantId, InventoryMovementType type,
            Pageable pageable) {
        productVariantService.getByIdOrThrow(productVariantId);
        return inventoryMovementRepository.findByProductVariantIdAndOptionalType(productVariantId, type, pageable);
    }

    // Gọi bởi StockReceiptService.confirm cho từng dòng phiếu nhập.
    @Transactional
    public InventoryBalance receiveStock(Long productVariantId, int quantity, String referenceType,
            Long referenceId, Long actingUserId) {
        productVariantService.getByIdOrThrow(productVariantId);
        return applyOnHandChange(productVariantId, quantity, InventoryMovementType.RECEIPT, referenceType,
                referenceId, null, actingUserId);
    }

    // Sẵn sàng cho Giai đoạn 6 (trả hàng tốt), chưa có caller.
    @Transactional
    public InventoryBalance receiveReturn(Long productVariantId, int quantity, String referenceType,
            Long referenceId) {
        productVariantService.getByIdOrThrow(productVariantId);
        return applyOnHandChange(productVariantId, quantity, InventoryMovementType.RETURN, referenceType,
                referenceId, null, null);
    }

    @Transactional
    public InventoryBalance adjust(Long productVariantId, int delta, String reason, Long actingUserId) {
        if (delta == 0) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Số lượng điều chỉnh phải khác 0");
        }
        if (reason == null || reason.isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Lý do điều chỉnh không được để trống");
        }
        productVariantService.getByIdOrThrow(productVariantId);
        InventoryMovementType type = delta > 0 ? InventoryMovementType.ADJUSTMENT_IN
                : InventoryMovementType.ADJUSTMENT_OUT;
        return applyOnHandChange(productVariantId, delta, type, null, null, reason, actingUserId);
    }

    // Sẵn sàng cho Giai đoạn 5 (Cart/Checkout), chưa có caller.
    @Transactional
    public InventoryBalance reserve(Long productVariantId, int quantity, String referenceType, Long referenceId) {
        productVariantService.getByIdOrThrow(productVariantId);
        int rows = inventoryBalanceRepository.reserveQuantity(productVariantId, quantity);
        if (rows == 0) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
        }
        InventoryBalance after = fetchBalance(productVariantId);
        recordMovement(after, InventoryMovementType.RESERVE, quantity, referenceType, referenceId, null, null);
        return after;
    }

    // Sẵn sàng cho Giai đoạn 5/6 (hủy đơn), chưa có caller.
    @Transactional
    public InventoryBalance release(Long productVariantId, int quantity, String referenceType, Long referenceId) {
        productVariantService.getByIdOrThrow(productVariantId);
        int rows = inventoryBalanceRepository.releaseQuantity(productVariantId, quantity);
        if (rows == 0) {
            throw new AppException(ErrorCode.RESOURCE_CONFLICT, "Số lượng giải phóng vượt quá số đã giữ");
        }
        InventoryBalance after = fetchBalance(productVariantId);
        recordMovement(after, InventoryMovementType.RELEASE, quantity, referenceType, referenceId, null, null);
        return after;
    }

    // Sẵn sàng cho Giai đoạn 6 (xuất đơn), chưa có caller.
    @Transactional
    public InventoryBalance fulfill(Long productVariantId, int quantity, String referenceType, Long referenceId) {
        productVariantService.getByIdOrThrow(productVariantId);
        int rows = inventoryBalanceRepository.fulfillQuantity(productVariantId, quantity);
        if (rows == 0) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
        }
        InventoryBalance after = fetchBalance(productVariantId);
        recordMovement(after, InventoryMovementType.SHIPMENT, quantity, referenceType, referenceId, null, null);
        return after;
    }

    private InventoryBalance applyOnHandChange(Long productVariantId, int delta, InventoryMovementType type,
            String referenceType, Long referenceId, String reason, Long actingUserId) {
        getOrCreateBalance(productVariantId);
        int rows = inventoryBalanceRepository.applyOnHandDelta(productVariantId, delta);
        if (rows == 0) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
        }
        InventoryBalance after = fetchBalance(productVariantId);
        recordMovement(after, type, Math.abs(delta), referenceType, referenceId, reason, actingUserId);
        return after;
    }

    private void recordMovement(InventoryBalance after, InventoryMovementType type, int quantity,
            String referenceType, Long referenceId, String reason, Long actingUserId) {
        inventoryMovementRepository.save(InventoryMovement.create(after.getProductVariantId(), type, quantity,
                after.getOnHandQuantity(), after.getReservedQuantity(), referenceType, referenceId, reason,
                actingUserId));
    }

    private InventoryBalance fetchBalance(Long productVariantId) {
        return inventoryBalanceRepository.findByProductVariantId(productVariantId).orElseThrow();
    }

    // Xử lý race lần tạo dòng balance đầu tiên: 2 giao dịch cùng nhập kho lần
    // đầu cho cùng 1 variant gần như đồng thời — bên thua cuộc đọc lại dòng đã
    // được tạo thay vì lỗi.
    private InventoryBalance getOrCreateBalance(Long productVariantId) {
        return inventoryBalanceRepository.findByProductVariantId(productVariantId).orElseGet(() -> {
            try {
                return inventoryBalanceRepository.save(InventoryBalance.create(productVariantId));
            } catch (DataIntegrityViolationException e) {
                return inventoryBalanceRepository.findByProductVariantId(productVariantId).orElseThrow(() -> e);
            }
        });
    }
}
