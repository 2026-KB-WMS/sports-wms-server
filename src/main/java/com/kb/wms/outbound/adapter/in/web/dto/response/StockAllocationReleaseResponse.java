package com.kb.wms.outbound.adapter.in.web.dto.response;

import java.time.LocalDateTime;

import com.kb.wms.outbound.application.port.in.result.StockAllocationReleaseResult;
import com.kb.wms.outbound.domain.enums.AllocationStatus;

/** PATCH /api/v1/allocations/{allocationId}/release 응답. */
public record StockAllocationReleaseResponse(
        Long allocationId,
        AllocationStatus status,
        Long allocatedQuantity,
        LocalDateTime releasedAt,
        Inventory inventory
) {

    public record Inventory(Long inventoryLotId, Long onHandQuantity, Long allocatedQuantity,
            Long availableQuantity) {
    }

    public static StockAllocationReleaseResponse from(StockAllocationReleaseResult r) {
        return new StockAllocationReleaseResponse(r.allocationId(), r.status(), r.allocatedQuantity(),
                r.releasedAt(), new Inventory(r.inventoryLotId(), r.onHandQuantity(),
                        r.inventoryAllocatedQuantity(), r.availableQuantity()));
    }
}
