package com.kb.wms.outbound.application.port.in.result;

import java.time.LocalDateTime;

import com.kb.wms.outbound.domain.enums.AllocationStatus;

/**
 * 재고 할당 해제 결과. 재고 수량은 해제 반영 뒤 재고 행의 값이다.
 */
public record StockAllocationReleaseResult(
        Long allocationId,
        AllocationStatus status,
        Long allocatedQuantity,
        LocalDateTime releasedAt,
        Long inventoryLotId,
        Long onHandQuantity,
        Long inventoryAllocatedQuantity,
        Long availableQuantity
) {
}