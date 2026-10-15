package com.kb.wms.inbound.adapter.in.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.kb.wms.inbound.application.port.in.result.PurchaseOrderView;
import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;

/**
 * GET /api/v1/purchase-orders/{purchaseOrderId} (발주 헤더 조회) 응답.
 * 취소·반려 사유(cancelReason, rejectReason)는 해당 상태일 때 StatusHistory에서 읽은 값(아니면 null)이다.
 * supplierOrders는 확정으로 만들어진 공급처 발주서이며 확정 전에는 빈 배열이다.
 */
public record PurchaseOrderResponse(
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
        List<PurchaseOrderSupplierOrderResponse> supplierOrders
) {

    public static PurchaseOrderResponse from(PurchaseOrderView view) {
        return new PurchaseOrderResponse(
                view.purchaseOrderId(),
                view.purchaseOrderNo(),
                view.warehouseId(),
                view.warehouseName(),
                view.status(),
                view.expectedAt(),
                view.note(),
                view.lineCount(),
                view.totalAmount(),
                view.createdBy(),
                view.createdByName(),
                view.createdAt(),
                view.updatedAt(),
                view.cancelReason(),
                view.rejectReason(),
                view.supplierOrders().stream().map(PurchaseOrderSupplierOrderResponse::from).toList());
    }
}
