package com.kb.wms.outbound.application.port.in.result;

import java.time.LocalDateTime;

import com.kb.wms.outbound.domain.enums.OutboundStatus;

/** 배송 시작 결과(PATCH /api/v1/outbounds/{outboundId}/ship). */
public record OutboundShipResult(
        Long outboundId,
        String outboundNo,
        OutboundStatus status,
        LocalDateTime shippedAt,
        Long shippedBy,
        LocalDateTime updatedAt
) {
}
