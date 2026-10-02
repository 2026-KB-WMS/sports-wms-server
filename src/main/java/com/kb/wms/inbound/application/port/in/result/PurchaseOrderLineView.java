package com.kb.wms.inbound.application.port.in.result;

import java.math.BigDecimal;

import com.kb.wms.inbound.domain.enums.PurchaseOrderLineStatus;

/**
 * 발주 항목 한 행 (SKU별 수량).
 *
 * @param remainingQuantity 아직 입고되지 않은 수량. 초과 입고여도 음수가 되지 않는다(최소 0).
 */
public record PurchaseOrderLineView(
        Long purchaseOrderLineId,
        Long skuId,
        String skuCode,
        String skuName,
        String unit,
        Long expectedQuantity,
        Long receivedQuantity,
        Long remainingQuantity,
        BigDecimal orderedUnitPrice,
        BigDecimal lineAmount,
        PurchaseOrderLineStatus status
) {
}
