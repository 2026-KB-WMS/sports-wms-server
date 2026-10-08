package com.kb.wms.outbound.adapter.in.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.kb.wms.outbound.application.port.in.result.OutboundDetail;
import com.kb.wms.outbound.application.port.in.result.OutboundLineView;
import com.kb.wms.outbound.application.port.in.result.OutboundView;
import com.kb.wms.outbound.domain.enums.OutboundStatus;

/**
 * GET /api/v1/outbounds/{outboundId}/details 응답. 확정 공급 단가와 금액은 피킹 완료 전에는 null이다
 * (이 API는 HQ_ADMIN·WAREHOUSE_MANAGER만 호출할 수 있다).
 */
public record OutboundDetailResponse(
        Long outboundId,
        String outboundNo,
        OutboundStatus status,
        Long storeOrderId,
        String orderNo,
        Long storeId,
        String storeName,
        Long warehouseId,
        String warehouseName,
        LocalDateTime shippedAt,
        Long shippedBy,
        LocalDateTime deliveredAt,
        String note,
        String cancelReason,
        List<Item> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public record Item(
            Long outboundLineId,
            Long allocationId,
            Long storeOrderLineId,
            Long skuId,
            String skuCode,
            String skuName,
            String unit,
            Long inventoryLotId,
            Long lotId,
            String lotNumber,
            LocalDate expiryDate,
            Long sectionId,
            String sectionCode,
            Long allocatedQuantity,
            Long shippedQuantity,
            BigDecimal confirmedUnitSupplyPrice,
            BigDecimal lineAmount
    ) {

        static Item from(OutboundLineView l) {
            return new Item(l.outboundLineId(), l.allocationId(), l.storeOrderLineId(), l.skuId(), l.skuCode(),
                    l.skuName(), l.unit(), l.inventoryLotId(), l.lotId(), l.lotNumber(), l.expiryDate(),
                    l.sectionId(), l.sectionCode(), l.allocatedQuantity(), l.shippedQuantity(),
                    l.confirmedUnitSupplyPrice(), l.lineAmount());
        }
    }

    public static OutboundDetailResponse from(OutboundDetail detail) {
        OutboundView v = detail.view();
        return new OutboundDetailResponse(v.outboundId(), v.outboundNo(), v.status(), v.storeOrderId(),
                v.orderNo(), v.storeId(), v.storeName(), v.warehouseId(), v.warehouseName(), v.shippedAt(),
                v.shippedBy(), v.deliveredAt(), v.note(), detail.cancelReason(),
                detail.items().stream().map(Item::from).toList(), v.createdAt(), v.updatedAt());
    }
}
