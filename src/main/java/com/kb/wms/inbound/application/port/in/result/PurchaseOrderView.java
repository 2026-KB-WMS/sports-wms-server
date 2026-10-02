package com.kb.wms.inbound.application.port.in.result;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;

/**
 * 발주 헤더 상세 (GET /api/v1/purchase-orders/{purchaseOrderId}).
 * 취소 사유(cancelReason)는 StatusHistory 도입 후 서비스에서 채워 응답에 합친다.
 *
 * @param lineCount   발주 항목 수
 * @param totalAmount 발주 항목 금액(line_amount) 합계
 */
public record PurchaseOrderView(
        Long purchaseOrderId,
        String purchaseOrderNo,
        Long warehouseId,
        String warehouseName,
        Long supplierId,
        String supplierName,
        PurchaseOrderStatus status,
        LocalDateTime expectedAt,
        String note,
        Long lineCount,
        BigDecimal totalAmount,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
