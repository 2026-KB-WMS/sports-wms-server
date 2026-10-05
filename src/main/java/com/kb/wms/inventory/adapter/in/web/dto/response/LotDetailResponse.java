package com.kb.wms.inventory.adapter.in.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.kb.wms.inventory.application.port.in.result.LotInboundView;
import com.kb.wms.inventory.application.port.in.result.LotSummary;
import com.kb.wms.inventory.domain.enums.LotStatus;
import com.kb.wms.inventory.domain.enums.QualityStatus;

/**
 * GET /api/v1/lots/{lotId} 응답.
 * inventory는 구역별 재고, inbounds는 InboundLine.lot_id가 이 로트인 입고 이력(입고 완료 건)이며 없으면 빈 배열이다.
 */
public record LotDetailResponse(
        Long lotId,
        String lotNumber,
        Long skuId,
        String skuCode,
        String skuName,
        Long supplierId,
        String supplierName,
        LocalDate manufacturedDate,
        LocalDate expiryDate,
        LotStatus status,
        BigDecimal unitCost,
        List<InventoryItem> inventory,
        List<InboundItem> inbounds,
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

    public record InboundItem(
            Long inboundId,
            String inboundNo,
            Long warehouseId,
            LocalDateTime receivedAt,
            Long receivedQuantity,
            Long acceptedQuantity,
            Long defectiveQuantity,
            BigDecimal receivedUnitPrice
    ) {

        public static InboundItem from(LotInboundView view) {
            return new InboundItem(
                    view.inboundId(),
                    view.inboundNo(),
                    view.warehouseId(),
                    view.receivedAt(),
                    view.receivedQuantity(),
                    view.acceptedQuantity(),
                    view.defectiveQuantity(),
                    view.receivedUnitPrice());
        }
    }

    public static LotDetailResponse of(LotSummary summary, List<InventoryItem> inventory,
                                       List<InboundItem> inbounds) {
        return new LotDetailResponse(
                summary.lotId(),
                summary.lotNumber(),
                summary.skuId(),
                summary.skuCode(),
                summary.skuName(),
                summary.supplierId(),
                summary.supplierName(),
                summary.manufacturedDate(),
                summary.expiryDate(),
                summary.status(),
                summary.unitCost(),
                inventory,
                inbounds,
                summary.createdAt(),
                summary.updatedAt());
    }
}
