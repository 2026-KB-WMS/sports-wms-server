package com.kb.wms.outbound.adapter.in.web.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.kb.wms.outbound.application.port.in.result.StockAllocationDetail;
import com.kb.wms.outbound.application.port.in.result.StockAllocationView;
import com.kb.wms.outbound.domain.enums.AllocationStatus;

/**
 * GET /api/v1/allocations/{allocationId} 응답. 회원 도메인이 없어 {@code allocatedByName}은 당분간 null이다.
 * {@code outboundId}는 취소되지 않은 출고에 연결된 경우에만 값이 있다.
 */
public record StockAllocationDetailResponse(
        Long allocationId,
        AllocationStatus status,
        Long allocatedQuantity,
        Long pickedQuantity,
        LocalDateTime allocatedAt,
        Long allocatedBy,
        String allocatedByName,
        LocalDateTime releasedAt,
        Long storeOrderId,
        String orderNo,
        Long storeId,
        String storeName,
        Long warehouseId,
        Long storeOrderLineId,
        Long requestedQuantity,
        Long skuId,
        String skuCode,
        String skuName,
        Long inventoryLotId,
        Long lotId,
        String lotNumber,
        LocalDate expiryDate,
        Long sectionId,
        String sectionCode,
        String sectionName,
        Long outboundId
) {

    public static StockAllocationDetailResponse from(StockAllocationDetail detail) {
        StockAllocationView v = detail.view();
        return new StockAllocationDetailResponse(v.allocationId(), v.status(), v.allocatedQuantity(),
                v.pickedQuantity(), v.allocatedAt(), v.allocatedBy(), null, v.releasedAt(), v.storeOrderId(),
                v.orderNo(), v.storeId(), v.storeName(), v.warehouseId(), v.storeOrderLineId(),
                v.requestedQuantity(), v.skuId(), v.skuCode(), v.skuName(), v.inventoryLotId(), v.lotId(),
                v.lotNumber(), v.expiryDate(), v.sectionId(), v.sectionCode(), v.sectionName(),
                detail.outboundId());
    }
}
