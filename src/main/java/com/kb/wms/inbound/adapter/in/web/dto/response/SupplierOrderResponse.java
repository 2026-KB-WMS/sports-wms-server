package com.kb.wms.inbound.adapter.in.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.kb.wms.inbound.application.port.in.result.SupplierOrderDetails;
import com.kb.wms.inbound.domain.enums.SupplierOrderStatus;

/**
 * GET /api/v1/supplier-orders/{supplierOrderId} (공급처 발주서 상세) 응답: 헤더 + 항목.
 */
public record SupplierOrderResponse(
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
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<SupplierOrderLineResponse> lines
) {

    public static SupplierOrderResponse from(SupplierOrderDetails details) {
        var header = details.header();
        return new SupplierOrderResponse(
                header.supplierOrderId(), header.supplierOrderNo(), header.purchaseOrderId(),
                header.purchaseOrderNo(), header.supplierId(), header.supplierName(), header.warehouseId(),
                header.warehouseName(), header.status(), header.lineCount(), header.totalAmount(),
                header.createdAt(), header.updatedAt(),
                details.lines().stream().map(SupplierOrderLineResponse::from).toList());
    }
}
