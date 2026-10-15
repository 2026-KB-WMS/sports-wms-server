package com.kb.wms.inbound.application.port.in.result;

import java.math.BigDecimal;

import com.kb.wms.inbound.domain.enums.PurchaseOrderLineStatus;

/**
 * 발주 항목 한 행 (SKU별 수량).
 *
 * @param remainingQuantity 아직 입고되지 않은 수량. 초과 입고여도 음수가 되지 않는다(최소 0).
 * @param supplierId        라인에 배정된 공급처. 본사가 확정해 지정하기 전에는 null이다.
 * @param supplierName      배정된 공급처명. 공급처가 없으면 null이다.
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
        PurchaseOrderLineStatus status,
        Long supplierId,
        String supplierName
) {
}
