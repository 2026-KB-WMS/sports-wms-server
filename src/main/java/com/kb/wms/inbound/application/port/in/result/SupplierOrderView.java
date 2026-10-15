package com.kb.wms.inbound.application.port.in.result;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.kb.wms.inbound.domain.enums.SupplierOrderStatus;

/**
 * 공급처 발주서 상세 헤더 (GET /api/v1/supplier-orders/{supplierOrderId}). 항목은 {@link SupplierOrderLineView}로 따로 조회한다.
 */
public record SupplierOrderView(
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
        LocalDateTime updatedAt
) {
}
