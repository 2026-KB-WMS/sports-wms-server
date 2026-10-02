package com.kb.wms.inbound.application.port.in.command;

/**
 * PATCH /api/v1/inbounds/{inboundId}/cancel 요청.
 *
 * @param reason 취소 사유 (필수, 최대 500자)
 */
public record InboundCancelCommand(
        String reason
) {
}
