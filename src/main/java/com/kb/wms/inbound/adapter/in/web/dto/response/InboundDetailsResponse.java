package com.kb.wms.inbound.adapter.in.web.dto.response;

import java.util.List;

import com.kb.wms.inbound.application.port.in.result.InboundDetails;
import com.kb.wms.inbound.domain.enums.InboundStatus;

/**
 * GET /api/v1/inbounds/{inboundId}/details (입고 상세, 검수 항목) 응답.
 */
public record InboundDetailsResponse(
        Long inboundId,
        String inboundNo,
        InboundStatus status,
        List<InboundLineResponse> items
) {

    public static InboundDetailsResponse from(InboundDetails details) {
        return new InboundDetailsResponse(
                details.inboundId(), details.inboundNo(), details.status(),
                details.items().stream().map(InboundLineResponse::from).toList());
    }
}
