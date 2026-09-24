package com.kb.wms.inventory.adapter.in.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.kb.wms.inventory.application.port.in.result.InventoryDetail;
import com.kb.wms.inventory.domain.enums.LotStatus;
import com.kb.wms.inventory.domain.enums.QualityStatus;

/**
 * GET /api/v1/inventory/{inventoryId} 응답.
 * supplierName은 Supplier(입고 도메인) 테이블이 생기면 채운다. 지금은 supplierId만 내려준다.
 */
public record InventoryDetailResponse(
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

    public static InventoryDetailResponse from(InventoryDetail detail) {
        return new InventoryDetailResponse(
                detail.inventoryLotId(),
                detail.warehouseId(),
                detail.warehouseName(),
                detail.sectionId(),
                detail.sectionCode(),
                detail.sectionName(),
                detail.skuId(),
                detail.skuCode(),
                detail.skuName(),
                detail.unit(),
                detail.lotId(),
                detail.lotNumber(),
                detail.supplierId(),
                detail.manufacturedDate(),
                detail.expiryDate(),
                detail.lotStatus(),
                detail.unitCost(),
                detail.onHandQuantity(),
                detail.allocatedQuantity(),
                detail.availableQuantity(),
                detail.qualityStatus(),
                detail.lastCountedAt(),
                detail.createdAt(),
                detail.updatedAt());
    }
}
