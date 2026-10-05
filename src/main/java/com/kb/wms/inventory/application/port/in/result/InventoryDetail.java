package com.kb.wms.inventory.application.port.in.result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.kb.wms.inventory.domain.enums.LotStatus;
import com.kb.wms.inventory.domain.enums.QualityStatus;

/**
 * 재고 한 건 상세 (GET /api/v1/inventory/{inventoryId}).
 * 공급처명(supplierName)은 supplier 테이블을 ID로 조인해 읽는다(ADR-007).
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
        String supplierName,
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
