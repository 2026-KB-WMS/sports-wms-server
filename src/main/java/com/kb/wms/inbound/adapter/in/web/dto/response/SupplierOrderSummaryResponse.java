package com.kb.wms.inbound.adapter.in.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.kb.wms.inbound.application.port.in.result.SupplierOrderSummary;
import com.kb.wms.inbound.domain.enums.SupplierOrderStatus;

/**
 * GET /api/v1/supplier-orders (공급처 발주서 목록) 응답 항목.
 */
public record SupplierOrderSummaryResponse(
        Long supplierOrderId,
        String supplierOrderNo,
        Long purchaseOrderId,
        String purchaseOrderNo,
        Long supplierId,
        String supplierName,
        Long warehouseId,
        String warehouseName,
        SupplierOrderStatus status,
        Long lineCount,
        BigDecimal totalAmount,
        LocalDateTime createdAt
) {

    public static SupplierOrderSummaryResponse from(SupplierOrderSummary summary) {
        return new SupplierOrderSummaryResponse(
                summary.supplierOrderId(), summary.supplierOrderNo(), summary.purchaseOrderId(),
                summary.purchaseOrderNo(), summary.supplierId(), summary.supplierName(), summary.warehouseId(),
                summary.warehouseName(), summary.status(), summary.lineCount(), summary.totalAmount(),
                summary.createdAt());
    }
}
