package com.laptophub.inventory.repository;

import com.laptophub.inventory.entity.StockReceiptItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface StockReceiptItemRepository extends JpaRepository<StockReceiptItem, Long> {

    List<StockReceiptItem> findByStockReceiptId(Long stockReceiptId);

    // Dùng bởi StockReceiptService.replaceItems (full-replace: xoá hết rồi tạo lại).
    @Modifying(clearAutomatically = true)
    void deleteByStockReceiptId(Long stockReceiptId);
}
