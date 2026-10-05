package com.kb.wms.outbound.application.port.in.result;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.kb.wms.outbound.domain.enums.AllocationStatus;

/**
 * 재고 할당 상세 (GET /api/v1/allocations/{allocationId}). 연결된 출고 ID는 별도 조회로 채운다.
 */
public record StockAllocationView(
        Long allocationId,
        AllocationStatus status,
        Long allocatedQuantity,
        Long pickedQuantity,
        LocalDateTime allocatedAt,
        Long allocatedBy,
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
        String sectionName
) {
}