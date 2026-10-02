package com.kb.wms.inbound.adapter.in.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.kb.wms.inbound.application.port.in.result.PurchaseOrderLineView;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderView;
import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;

/**
 * POST /api/v1/purchase-orders (발주 등록) 응답: 등록된 발주 헤더 + 항목.
 */
public record PurchaseOrderRegisterResponse(
        Long purchaseOrderId,
        String purchaseOrderNo,
        Long warehouseId,
        String warehouseName,
        Long supplierId,
        String supplierName,
        PurchaseOrderStatus status,
        LocalDateTime expectedAt,
        String note,
        BigDecimal totalAmount,
        Long createdBy,
        LocalDateTime createdAt,
        List<PurchaseOrderLineResponse> lines
) {

    public static PurchaseOrderRegisterResponse of(PurchaseOrderView view, List<PurchaseOrderLineView> lines) {
        return new PurchaseOrderRegisterResponse(
                view.purchaseOrderId(),
                view.purchaseOrderNo(),
                view.warehouseId(),
                view.warehouseName(),
                view.supplierId(),
                view.supplierName(),
                view.status(),
                view.expectedAt(),
                view.note(),
                view.totalAmount(),
                view.createdBy(),
                view.createdAt(),
                lines.stream().map(PurchaseOrderLineResponse::from).toList());
    }
}
