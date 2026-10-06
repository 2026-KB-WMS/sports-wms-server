package com.kb.wms.inbound.adapter.in.web.dto.response;

import java.time.LocalDateTime;

import com.kb.wms.inbound.application.port.in.result.InboundView;
import com.kb.wms.inbound.domain.enums.InboundStatus;

/**
 * GET /api/v1/inbounds/{inboundId} (입고 단건) 응답.
 * 취소 사유(cancelReason)는 취소 상태일 때 StatusHistory에서 읽은 값(아니면 null)이다.
 */
public record InboundResponse(
        Long inboundId,
        String inboundNo,
        Long purchaseOrderId,
        String purchaseOrderNo,
        Long supplierId,
        String supplierName,
        Long warehouseId,
        String warehouseName,
        InboundStatus status,
        LocalDateTime arrivedAt,
        LocalDateTime receivedAt,
        Long receivedBy,
        String receivedByName,
        String note,
        Long lineCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String cancelReason
) {

    public static InboundResponse from(InboundView view) {
        return new InboundResponse(
                view.inboundId(), view.inboundNo(), view.purchaseOrderId(), view.purchaseOrderNo(),
                view.supplierId(), view.supplierName(), view.warehouseId(), view.warehouseName(),
                view.status(), view.arrivedAt(), view.receivedAt(), view.receivedBy(), view.receivedByName(), view.note(),
                view.lineCount(), view.createdAt(), view.updatedAt(), view.cancelReason());
    }
}
