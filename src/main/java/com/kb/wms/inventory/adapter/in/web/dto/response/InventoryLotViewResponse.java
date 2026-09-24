package com.kb.wms.inventory.adapter.in.web.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.kb.wms.inventory.application.port.in.result.InventoryLotView;
import com.kb.wms.inventory.domain.enums.QualityStatus;

/**
 * GET /api/v1/inventory/by-lot 목록 항목 (로트·구역 단위 재고 상세).
 */
public record InventoryLotViewResponse(
        Long inventoryLotId,
        Long lotId,
        String lotNumber,
        Long skuId,
        String skuCode,
        String skuName,
        Long warehouseId,
        Long sectionId,
        String sectionCode,
        String sectionName,
        Long onHandQuantity,
        Long allocatedQuantity,
        Long availableQuantity,
        QualityStatus qualityStatus,
        LocalDate expiryDate,
        LocalDateTime lastCountedAt
) {

    public static InventoryLotViewResponse from(InventoryLotView view) {
        return new InventoryLotViewResponse(
                view.inventoryLotId(),
                view.lotId(),
                view.lotNumber(),
                view.skuId(),
                view.skuCode(),
                view.skuName(),
                view.warehouseId(),
                view.sectionId(),
                view.sectionCode(),
                view.sectionName(),
                view.onHandQuantity(),
                view.allocatedQuantity(),
                view.availableQuantity(),
                view.qualityStatus(),
                view.expiryDate(),
                view.lastCountedAt());
    }
}
