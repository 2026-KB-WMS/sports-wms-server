package com.kb.wms.inbound.adapter.in.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.kb.wms.inbound.application.port.in.result.PurchaseOrderSummary;
import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;

/**
 * GET /api/v1/purchase-orders (발주 목록) 응답 항목. suppliers는 라인에 배정된 공급처이며 확정 전에는 빈 배열이다.
 */
public record PurchaseOrderSummaryResponse(
        Long purchaseOrderId,
        String purchaseOrderNo,
        Long warehouseId,
        String warehouseName,
        PurchaseOrderStatus status,
        LocalDateTime expectedAt,
        Long lineCount,
        BigDecimal totalAmount,
        Long createdBy,
        String createdByName,
        LocalDateTime createdAt,
        List<PurchaseOrderSupplierResponse> suppliers
) {

    public static PurchaseOrderSummaryResponse from(PurchaseOrderSummary summary) {
        return new PurchaseOrderSummaryResponse(
                summary.purchaseOrderId(),
                summary.purchaseOrderNo(),
                summary.warehouseId(),
                summary.warehouseName(),
                summary.status(),
                summary.expectedAt(),
                summary.lineCount(),
                summary.totalAmount(),
                summary.createdBy(),
                summary.createdByName(),
                summary.createdAt(),
                summary.suppliers().stream().map(PurchaseOrderSupplierResponse::from).toList());
    }
}
