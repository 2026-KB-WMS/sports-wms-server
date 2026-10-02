package com.kb.wms.inbound.adapter.in.web.dto.response;

import java.time.LocalDateTime;

import com.kb.wms.inbound.application.port.in.result.InboundView;
import com.kb.wms.inbound.domain.enums.InboundStatus;

/**
 * POST /api/v1/inbounds (입고 등록) 응답: 등록된 입고 헤더.
 */
public record InboundRegisterResponse(
        Long inboundId,
        String inboundNo,
        Long purchaseOrderId,
        String purchaseOrderNo,
        Long warehouseId,
        String warehouseName,
        InboundStatus status,
        LocalDateTime arrivedAt,
        LocalDateTime receivedAt,
        Long receivedBy,
        String note,
        LocalDateTime createdAt
) {

    public static InboundRegisterResponse from(InboundView view) {
        return new InboundRegisterResponse(
                view.inboundId(), view.inboundNo(), view.purchaseOrderId(), view.purchaseOrderNo(),
                view.warehouseId(), view.warehouseName(), view.status(), view.arrivedAt(),
                view.receivedAt(), view.receivedBy(), view.note(), view.createdAt());
    }
}
