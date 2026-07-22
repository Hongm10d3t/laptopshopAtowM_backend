package com.laptophub.inventory.repository;

import com.laptophub.inventory.entity.StockReceipt;
import com.laptophub.inventory.entity.StockReceiptStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockReceiptRepository extends JpaRepository<StockReceipt, Long> {

    boolean existsByCode(String code);

    @Query("SELECT r FROM StockReceipt r WHERE (:status IS NULL OR r.status = :status) ORDER BY r.createdAt DESC")
    Page<StockReceipt> search(@Param("status") StockReceiptStatus status, Pageable pageable);
}
