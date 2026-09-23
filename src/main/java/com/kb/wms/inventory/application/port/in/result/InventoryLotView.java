package com.kb.wms.inventory.application.port.in.result;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.kb.wms.inventory.domain.enums.QualityStatus;

/**
 * 로트·구역 단위 재고 행 (GET /api/v1/inventory/by-lot, 로트 상세의 구역별 분포, 할당 후보).
 *
 * @param availableQuantity 품질·로트 상태가 모두 AVAILABLE이면 on_hand - allocated, 아니면 0
 */
public record InventoryLotView(
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
}
