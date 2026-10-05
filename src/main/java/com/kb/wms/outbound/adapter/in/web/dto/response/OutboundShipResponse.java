package com.kb.wms.outbound.adapter.in.web.dto.response;

import java.time.LocalDateTime;

import com.kb.wms.outbound.application.port.in.result.OutboundShipResult;
import com.kb.wms.outbound.domain.enums.OutboundStatus;

/** PATCH /api/v1/outbounds/{outboundId}/ship 응답. */
public record OutboundShipResponse(
        Long outboundId,
        String outboundNo,
        OutboundStatus status,
        LocalDateTime shippedAt,
        Long shippedBy,
        LocalDateTime updatedAt
) {

    public static OutboundShipResponse from(OutboundShipResult r) {
        return new OutboundShipResponse(r.outboundId(), r.outboundNo(), r.status(), r.shippedAt(), r.shippedBy(),
                r.updatedAt());
    }
}
