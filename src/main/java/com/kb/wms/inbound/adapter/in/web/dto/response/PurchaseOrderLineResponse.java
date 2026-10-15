package com.kb.wms.inbound.adapter.in.web.dto.response;

import java.math.BigDecimal;

import com.kb.wms.inbound.application.port.in.result.PurchaseOrderLineView;
import com.kb.wms.inbound.domain.enums.PurchaseOrderLineStatus;

/**
 * 발주 항목(SKU별 수량) 응답. 발주 등록 응답의 lines와 details 응답의 items에서 함께 쓴다.
 * 공급처와 공급처 발주서는 본사가 확정하기 전에는 null이다.
 */
public record PurchaseOrderLineResponse(
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
        String supplierName,
        Long supplierOrderId
) {

    public static PurchaseOrderLineResponse from(PurchaseOrderLineView line) {
        return new PurchaseOrderLineResponse(
                line.purchaseOrderLineId(),
                line.skuId(),
                line.skuCode(),
                line.skuName(),
                line.unit(),
                line.expectedQuantity(),
                line.receivedQuantity(),
                line.remainingQuantity(),
                line.orderedUnitPrice(),
                line.lineAmount(),
                line.status(),
                line.supplierId(),
                line.supplierName(),
                line.supplierOrderId());
    }
}
