package com.kb.wms.inbound.adapter.in.web.dto.response;

import java.time.LocalDateTime;

import com.kb.wms.inbound.application.port.in.result.InboundView;
import com.kb.wms.inbound.domain.enums.InboundStatus;

/**
 * GET /api/v1/inbounds/{inboundId} (입고 단건) 응답.
 * 처리자 이름(receivedByName)과 취소 사유(cancelReason)는 회원·StatusHistory 도메인 연동 후 추가한다.
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
        String note,
        Long lineCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static InboundResponse from(InboundView view) {
        return new InboundResponse(
                view.inboundId(), view.inboundNo(), view.purchaseOrderId(), view.purchaseOrderNo(),
                view.supplierId(), view.supplierName(), view.warehouseId(), view.warehouseName(),
                view.status(), view.arrivedAt(), view.receivedAt(), view.receivedBy(), view.note(),
                view.lineCount(), view.createdAt(), view.updatedAt());
    }
}
