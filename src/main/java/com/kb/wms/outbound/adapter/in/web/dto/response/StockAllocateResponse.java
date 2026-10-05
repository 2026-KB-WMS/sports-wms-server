package com.kb.wms.outbound.adapter.in.web.dto.response;

import java.util.List;

import com.kb.wms.outbound.application.port.in.result.StockAllocateResult;

/** POST /api/v1/allocations 응답. */
public record StockAllocateResponse(
        Long storeOrderId,
        String orderNo,
        List<StockAllocationItemResponse> items
) {

    public static StockAllocateResponse from(StockAllocateResult result) {
        return new StockAllocateResponse(result.storeOrderId(), result.orderNo(),
                result.items().stream().map(StockAllocationItemResponse::from).toList());
    }
}
