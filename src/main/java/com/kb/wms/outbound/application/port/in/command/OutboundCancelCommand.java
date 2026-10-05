package com.kb.wms.outbound.application.port.in.command;

/**
 * 출고 취소 요청(PATCH /api/v1/outbounds/{outboundId}/cancel).
 *
 * @param reason 취소 사유(필수, 500자 이하). 상태 이력에 저장한다.
 */
public record OutboundCancelCommand(
        Long outboundId,
        String reason,
        Long userId
) {
}
