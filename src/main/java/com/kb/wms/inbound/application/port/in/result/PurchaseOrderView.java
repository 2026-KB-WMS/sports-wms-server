package com.kb.wms.inbound.application.port.in.result;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;

/**
 * 발주 헤더 상세 (GET /api/v1/purchase-orders/{purchaseOrderId}).
 * 취소 사유(cancelReason)는 조회 쿼리에서 읽지 않고, 서비스가 CANCELED 발주에 대해 StatusHistory에서 읽어
 * {@link #withCancelReason(String)}으로 채운다.
 *
 * @param lineCount   발주 항목 수
 * @param totalAmount 발주 항목 금액(line_amount) 합계
 * @param cancelReason 취소 사유. 취소 상태가 아니거나 사유 없이 취소했으면 null
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
        String createdByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String cancelReason
) {

    /** 조회 쿼리(JPQL 생성자 표현식)용: 취소 사유 없이 만든다. */
    public PurchaseOrderView(Long purchaseOrderId, String purchaseOrderNo, Long warehouseId, String warehouseName,
                             Long supplierId, String supplierName, PurchaseOrderStatus status,
                             LocalDateTime expectedAt, String note, Long lineCount, BigDecimal totalAmount,
                             Long createdBy, String createdByName, LocalDateTime createdAt,
                             LocalDateTime updatedAt) {
        this(purchaseOrderId, purchaseOrderNo, warehouseId, warehouseName, supplierId, supplierName, status,
                expectedAt, note, lineCount, totalAmount, createdBy, createdByName, createdAt, updatedAt, null);
    }

    public PurchaseOrderView withCancelReason(String cancelReason) {
        return new PurchaseOrderView(purchaseOrderId, purchaseOrderNo, warehouseId, warehouseName, supplierId,
                supplierName, status, expectedAt, note, lineCount, totalAmount, createdBy, createdByName,
                createdAt, updatedAt, cancelReason);
    }
}
