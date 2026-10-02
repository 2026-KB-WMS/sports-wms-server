package com.kb.wms.inbound.application.port.in.result;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;

/**
 * 발주 목록 한 행 (GET /api/v1/purchase-orders).
 *
 * @param lineCount   발주 항목 수
 * @param totalAmount 발주 항목 금액(line_amount) 합계
 */
public record PurchaseOrderSummary(
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
        LocalDateTime createdAt
) {
}
