package com.kb.wms.inbound.application.port.in.result;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.kb.wms.inbound.domain.enums.SupplierOrderStatus;

/**
 * 공급처 발주서 목록 한 행 (GET /api/v1/supplier-orders).
 *
 * @param lineCount   발주서에 연결된 발주 항목 수
 * @param totalAmount 연결된 항목 금액(line_amount) 합계
 */
public record SupplierOrderSummary(
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
}
