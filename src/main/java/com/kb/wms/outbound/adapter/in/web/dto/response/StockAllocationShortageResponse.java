package com.kb.wms.outbound.adapter.in.web.dto.response;

import com.kb.wms.outbound.application.port.in.result.StockAllocationShortage;

/** 재고가 모자라 일부만 할당한 SKU. */
public record StockAllocationShortageResponse(
        Long skuId,
        long requestedQuantity,
        long allocatedQuantity,
        long shortageQuantity
) {

    public static StockAllocationShortageResponse from(StockAllocationShortage shortage) {
        return new StockAllocationShortageResponse(shortage.skuId(), shortage.requestedQuantity(),
                shortage.allocatedQuantity(), shortage.shortageQuantity());
    }
}
