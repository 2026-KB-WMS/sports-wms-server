package com.kb.wms.inventory.application.port.in.result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.kb.wms.inventory.domain.enums.LotStatus;
import com.kb.wms.inventory.domain.enums.QualityStatus;

/**
 * 재고 한 건 상세 (GET /api/v1/inventory/{inventoryId}).
 * 공급처명은 Supplier(입고 도메인) 테이블이 생기면 추가한다.
 */
public record InventoryDetail(
        Long inventoryLotId,
        Long warehouseId,
        String warehouseName,
        Long sectionId,
        String sectionCode,
        String sectionName,
        Long skuId,
        String skuCode,
        String skuName,
        String unit,
        Long lotId,
        String lotNumber,
        Long supplierId,
        LocalDate manufacturedDate,
        LocalDate expiryDate,
        LotStatus lotStatus,
        BigDecimal unitCost,
        Long onHandQuantity,
        Long allocatedQuantity,
        Long availableQuantity,
        QualityStatus qualityStatus,
        LocalDateTime lastCountedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
