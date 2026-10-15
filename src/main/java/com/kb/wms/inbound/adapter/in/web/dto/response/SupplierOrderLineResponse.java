package com.kb.wms.inbound.adapter.in.web.dto.response;

import java.math.BigDecimal;

import com.kb.wms.inbound.application.port.in.result.SupplierOrderLineView;
import com.kb.wms.inbound.domain.enums.PurchaseOrderLineStatus;

/**
 * 공급처 발주서에 연결된 발주 항목(SKU별 수량) 응답.
 */
public record SupplierOrderLineResponse(
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

    public static SupplierOrderLineResponse from(SupplierOrderLineView line) {
        return new SupplierOrderLineResponse(
                line.purchaseOrderLineId(), line.skuId(), line.skuCode(), line.skuName(), line.unit(),
                line.expectedQuantity(), line.receivedQuantity(), line.remainingQuantity(),
                line.orderedUnitPrice(), line.lineAmount(), line.status());
    }
}
