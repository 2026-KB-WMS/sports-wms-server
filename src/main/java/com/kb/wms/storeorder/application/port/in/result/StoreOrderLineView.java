package com.kb.wms.storeorder.application.port.in.result;

import java.math.BigDecimal;

import com.kb.wms.storeorder.domain.enums.StoreOrderLineStatus;

/**
 * 지점 발주 항목 한 행. SKU 코드·이름·단위는 조회 쿼리가 product_sku를 ID로 조인해 채운다.
 */
public record StoreOrderLineView(
        Long storeOrderLineId,
        Long skuId,
        String skuCode,
        String skuName,
        String unit,
        Long requestedQuantity,
        Long allocatedQuantity,
        Long shippedQuantity,
        BigDecimal requestedUnitSupplyPrice,
        StoreOrderLineStatus status
) {

    /** 항목 금액 = 요청 수량 × 공급 단가. 저장하지 않고 계산한다. */
    public BigDecimal lineAmount() {
        return requestedUnitSupplyPrice.multiply(BigDecimal.valueOf(requestedQuantity));
    }

    /** 아직 출고되지 않은 수량(부족분). 초과 출고여도 음수가 되지 않는다. */
    public long remainingQuantity() {
        return Math.max(0L, requestedQuantity - shippedQuantity);
    }
}
