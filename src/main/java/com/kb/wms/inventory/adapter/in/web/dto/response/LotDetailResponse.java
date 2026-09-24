package com.kb.wms.inventory.adapter.in.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.kb.wms.inventory.application.port.in.result.LotSummary;
import com.kb.wms.inventory.domain.enums.LotStatus;
import com.kb.wms.inventory.domain.enums.QualityStatus;

/**
 * GET /api/v1/lots/{lotId} 응답.
 * supplierName은 Supplier(입고 도메인) 테이블이 생기면 채운다.
 * inbounds는 입고(Inbound) 도메인이 아직 없어 항상 빈 배열로 내려준다. 입고 도메인 구현 시 채운다.
 */
public record LotDetailResponse(
        Long lotId,
        String lotNumber,
        Long skuId,
        String skuCode,
        String skuName,
        Long supplierId,
        LocalDate manufacturedDate,
        LocalDate expiryDate,
        LotStatus status,
        BigDecimal unitCost,
        List<InventoryItem> inventory,
        List<Object> inbounds,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public record InventoryItem(
            Long inventoryLotId,
            Long warehouseId,
            String warehouseName,
            Long sectionId,
            String sectionCode,
            String sectionName,
            Long onHandQuantity,
            Long allocatedQuantity,
            QualityStatus qualityStatus
    ) {
    }

    public static LotDetailResponse of(LotSummary summary, List<InventoryItem> inventory) {
        return new LotDetailResponse(
                summary.lotId(),
                summary.lotNumber(),
                summary.skuId(),
                summary.skuCode(),
                summary.skuName(),
                summary.supplierId(),
                summary.manufacturedDate(),
                summary.expiryDate(),
                summary.status(),
                summary.unitCost(),
                inventory,
                List.of(),
                summary.createdAt(),
                summary.updatedAt());
    }
}
