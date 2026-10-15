package com.kb.wms.inbound.application.port.in.result;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;

/**
 * 발주(요청) 헤더 상세 (GET /api/v1/purchase-orders/{purchaseOrderId}).
 * 취소·반려 사유와 공급처 발주서는 조회 쿼리에서 읽지 않고, 서비스가 StatusHistory와 발주서 조회로 채운다
 * ({@link #withCancelReason(String)}, {@link #withRejectReason(String)}, {@link #withSupplierOrders(List)}).
 *
 * @param lineCount      발주 항목 수
 * @param totalAmount    발주 항목 금액(line_amount) 합계
 * @param cancelReason   취소 사유. 취소 상태가 아니거나 사유 없이 취소했으면 null
 * @param rejectReason   반려 사유. 반려 상태가 아니면 null
 * @param supplierOrders 확정으로 만들어진 공급처 발주서. 확정 전에는 빈 목록
 */
public record PurchaseOrderView(
        Long purchaseOrderId,
        String purchaseOrderNo,
        Long warehouseId,
        String warehouseName,
        PurchaseOrderStatus status,
        LocalDateTime expectedAt,
        String note,
        Long lineCount,
        BigDecimal totalAmount,
        Long createdBy,
        String createdByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String cancelReason,
        String rejectReason,
        List<SupplierOrderSummary> supplierOrders
) {

    /** 조회 쿼리(JPQL 생성자 표현식)용: 사유와 공급처 발주서 없이 만든다. */
    public PurchaseOrderView(Long purchaseOrderId, String purchaseOrderNo, Long warehouseId, String warehouseName,
                             PurchaseOrderStatus status, LocalDateTime expectedAt, String note, Long lineCount,
                             BigDecimal totalAmount, Long createdBy, String createdByName, LocalDateTime createdAt,
                             LocalDateTime updatedAt) {
        this(purchaseOrderId, purchaseOrderNo, warehouseId, warehouseName, status, expectedAt, note, lineCount,
                totalAmount, createdBy, createdByName, createdAt, updatedAt, null, null, List.of());
    }

    public PurchaseOrderView withCancelReason(String cancelReason) {
        return new PurchaseOrderView(purchaseOrderId, purchaseOrderNo, warehouseId, warehouseName, status,
                expectedAt, note, lineCount, totalAmount, createdBy, createdByName, createdAt, updatedAt,
                cancelReason, rejectReason, supplierOrders);
    }

    public PurchaseOrderView withRejectReason(String rejectReason) {
        return new PurchaseOrderView(purchaseOrderId, purchaseOrderNo, warehouseId, warehouseName, status,
                expectedAt, note, lineCount, totalAmount, createdBy, createdByName, createdAt, updatedAt,
                cancelReason, rejectReason, supplierOrders);
    }

    public PurchaseOrderView withSupplierOrders(List<SupplierOrderSummary> supplierOrders) {
        return new PurchaseOrderView(purchaseOrderId, purchaseOrderNo, warehouseId, warehouseName, status,
                expectedAt, note, lineCount, totalAmount, createdBy, createdByName, createdAt, updatedAt,
                cancelReason, rejectReason, supplierOrders);
    }
}
