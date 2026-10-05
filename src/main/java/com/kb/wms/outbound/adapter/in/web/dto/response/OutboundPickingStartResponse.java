package com.kb.wms.outbound.adapter.in.web.dto.response;

import java.time.LocalDateTime;

import com.kb.wms.outbound.application.port.in.result.OutboundPickingStartResult;
import com.kb.wms.outbound.domain.enums.OutboundStatus;

/** PATCH /api/v1/outbounds/{outboundId}/picking/start 응답. */
public record OutboundPickingStartResponse(
        Long outboundId,
        String outboundNo,
        OutboundStatus status,
        LocalDateTime updatedAt
) {

    public static OutboundPickingStartResponse from(OutboundPickingStartResult r) {
        return new OutboundPickingStartResponse(r.outboundId(), r.outboundNo(), r.status(), r.updatedAt());
    }
}
