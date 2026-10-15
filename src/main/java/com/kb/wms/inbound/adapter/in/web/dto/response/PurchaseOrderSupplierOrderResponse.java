package com.kb.wms.inbound.adapter.in.web.dto.response;

import java.math.BigDecimal;

import com.kb.wms.inbound.application.port.in.result.SupplierOrderSummary;
import com.kb.wms.inbound.domain.enums.SupplierOrderStatus;

/**
 * 발주(요청) 응답에 담기는 공급처 발주서 요약. 확정 응답과 발주 상세에서 함께 쓴다.
 */
public record PurchaseOrderSupplierOrderResponse(
        Long supplierOrderId,
        String supplierOrderNo,
        Long supplierId,
        String supplierName,
        SupplierOrderStatus status,
        Long lineCount,
        BigDecimal totalAmount
) {

    public static PurchaseOrderSupplierOrderResponse from(SupplierOrderSummary summary) {
        return new PurchaseOrderSupplierOrderResponse(
                summary.supplierOrderId(), summary.supplierOrderNo(), summary.supplierId(), summary.supplierName(),
                summary.status(), summary.lineCount(), summary.totalAmount());
    }
}
