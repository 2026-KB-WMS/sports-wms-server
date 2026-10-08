package com.kb.wms.inbound.application.port.in.command;

/**
 * PATCH /api/v1/inbounds/{inboundId}/cancel 요청.
 *
 * @param reason 취소 사유 (필수, 최대 500자)
 * @param userId 취소 처리 사용자 (토큰 사용자의 ID). 상태 이력의 처리자로 기록한다
 */
public record InboundCancelCommand(
        String reason,
        Long userId
) {
}
