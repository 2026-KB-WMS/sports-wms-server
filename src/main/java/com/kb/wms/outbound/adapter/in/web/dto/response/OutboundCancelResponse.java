package com.kb.wms.outbound.adapter.in.web.dto.response;

import java.time.LocalDateTime;

import com.kb.wms.outbound.application.port.in.result.OutboundCancelResult;
import com.kb.wms.outbound.domain.enums.OutboundStatus;

/** PATCH /api/v1/outbounds/{outboundId}/cancel 응답. */
public record OutboundCancelResponse(
        Long outboundId,
        String outboundNo,
        OutboundStatus status,
        String cancelReason,
        LocalDateTime updatedAt
) {

    public static OutboundCancelResponse from(OutboundCancelResult r) {
        return new OutboundCancelResponse(r.outboundId(), r.outboundNo(), r.status(), r.cancelReason(),
                r.updatedAt());
    }
}
