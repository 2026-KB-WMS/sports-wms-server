package com.kb.wms.outbound.application.port.in.result;

import java.time.LocalDateTime;

import com.kb.wms.outbound.domain.enums.OutboundStatus;

/**
 * 피킹 시작 결과(PATCH /api/v1/outbounds/{outboundId}/picking/start).
 */
public record OutboundPickingStartResult(
        Long outboundId,
        String outboundNo,
        OutboundStatus status,
        LocalDateTime updatedAt
) {
}
