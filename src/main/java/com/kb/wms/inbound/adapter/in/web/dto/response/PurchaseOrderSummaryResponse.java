package com.kb.wms.inbound.adapter.in.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.kb.wms.inbound.application.port.in.result.PurchaseOrderSummary;
import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;

/**
 * GET /api/v1/purchase-orders (발주 목록) 응답 항목.
 */
public record PurchaseOrderSummaryResponse(
        Long purchaseOrderId,
        String purchaseOrderNo,
        Long warehouseId,
        String warehouseName,
        Long supplierId,
        String supplierName,
        PurchaseOrderStatus status,
        LocalDateTime expectedAt,
        Long lineCount,
        BigDecimal totalAmount,
        Long createdBy,
        String createdByName,
        LocalDateTime createdAt
) {

    public static PurchaseOrderSummaryResponse from(PurchaseOrderSummary summary) {
        return new PurchaseOrderSummaryResponse(
                summary.purchaseOrderId(),
                summary.purchaseOrderNo(),
                summary.warehouseId(),
                summary.warehouseName(),
                summary.supplierId(),
                summary.supplierName(),
                summary.status(),
                summary.expectedAt(),
                summary.lineCount(),
                summary.totalAmount(),
                summary.createdBy(),
                summary.createdByName(),
                summary.createdAt());
    }
}
