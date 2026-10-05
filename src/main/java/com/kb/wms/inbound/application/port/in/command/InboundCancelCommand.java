package com.kb.wms.inbound.application.port.in.command;

/**
 * PATCH /api/v1/inbounds/{inboundId}/cancel 요청.
 *
 * @param reason 취소 사유 (필수, 최대 500자)
 * @param userId 취소 처리 사용자 (인증 연동 전 임시로 쿼리 파라미터로 받는다). 상태 이력의 처리자로 기록한다
 */
public record InboundCancelCommand(
        String reason,
        Long userId
) {
}
