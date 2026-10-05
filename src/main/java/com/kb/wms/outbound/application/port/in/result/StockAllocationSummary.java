package com.kb.wms.outbound.application.port.in.result;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.kb.wms.outbound.domain.enums.AllocationStatus;

/**
 * 재고 할당 목록 한 줄 (GET /api/v1/allocations, 할당 생성 응답의 items). 발주·SKU·로트·구역 정보는 ID 기준 읽기 전용 조인이다.
 */
public record StockAllocationSummary(
        Long allocationId,
        Long storeOrderId,
        String orderNo,
        Long storeOrderLineId,
        Long warehouseId,
        Long skuId,
        String skuCode,
        String skuName,
        Long inventoryLotId,
        Long lotId,
        String lotNumber,
        LocalDate expiryDate,
        Long sectionId,
        String sectionCode,
        Long allocatedQuantity,
        Long pickedQuantity,
        AllocationStatus status,
        LocalDateTime allocatedAt,
        LocalDateTime releasedAt
) {
}