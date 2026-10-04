package com.kb.wms.storeorder.adapter.in.web.dto.response;

import java.math.BigDecimal;

import com.kb.wms.storeorder.application.port.in.result.StoreOrderLineView;
import com.kb.wms.storeorder.domain.enums.StoreOrderLineStatus;

/**
 * 발주 항목 한 행. 등록 응답의 lines[]와 상세 응답의 items[]가 함께 쓴다.
 */
public record StoreOrderLineResponse(
        Long storeOrderLineId,
        Long skuId,
        String skuCode,
        String skuName,
        String unit,
        Long requestedQuantity,
        Long allocatedQuantity,
        Long shippedQuantity,
        Long remainingQuantity,
        BigDecimal requestedUnitSupplyPrice,
        BigDecimal lineAmount,
        StoreOrderLineStatus status
) {

    public static StoreOrderLineResponse from(StoreOrderLineView line) {
        return new StoreOrderLineResponse(
                line.storeOrderLineId(),
                line.skuId(),
                line.skuCode(),
                line.skuName(),
                line.unit(),
                line.requestedQuantity(),
                line.allocatedQuantity(),
                line.shippedQuantity(),
                line.remainingQuantity(),
                line.requestedUnitSupplyPrice(),
                line.lineAmount(),
                line.status());
    }
}
