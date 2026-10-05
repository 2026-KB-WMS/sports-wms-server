package com.kb.wms.inbound.adapter.in.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.kb.wms.inbound.application.port.in.result.PurchaseOrderView;
import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;

/**
 * GET /api/v1/purchase-orders/{purchaseOrderId} (발주 헤더 조회) 응답.
 * 취소 사유(cancelReason)는 취소 상태일 때 StatusHistory에서 읽은 값(사유 없이 취소했거나 취소가 아니면 null)이다.
 * 작성자 이름(createdByName)은 회원 도메인 연동 후 추가한다.
 */
public record PurchaseOrderResponse(
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
        LocalDateTime updatedAt,
        String cancelReason
) {

    public static PurchaseOrderResponse from(PurchaseOrderView view) {
        return new PurchaseOrderResponse(
                view.purchaseOrderId(),
                view.purchaseOrderNo(),
                view.warehouseId(),
                view.warehouseName(),
                view.supplierId(),
                view.supplierName(),
                view.status(),
                view.expectedAt(),
                view.note(),
                view.lineCount(),
                view.totalAmount(),
                view.createdBy(),
                view.createdAt(),
                view.updatedAt(),
                view.cancelReason());
    }
}
