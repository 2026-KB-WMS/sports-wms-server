package com.kb.wms.outbound.adapter.in.web.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.kb.wms.outbound.application.port.in.result.StockAllocationSummary;
import com.kb.wms.outbound.domain.enums.AllocationStatus;

/** 재고 할당 한 건: POST /allocations 의 items[]와 GET /allocations 의 data.items[]. */
public record StockAllocationItemResponse(
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

    public static StockAllocationItemResponse from(StockAllocationSummary s) {
        return new StockAllocationItemResponse(s.allocationId(), s.storeOrderId(), s.orderNo(),
                s.storeOrderLineId(), s.warehouseId(), s.skuId(), s.skuCode(), s.skuName(), s.inventoryLotId(),
                s.lotId(), s.lotNumber(), s.expiryDate(), s.sectionId(), s.sectionCode(), s.allocatedQuantity(),
                s.pickedQuantity(), s.status(), s.allocatedAt(), s.releasedAt());
    }
}
