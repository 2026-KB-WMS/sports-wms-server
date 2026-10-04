package com.kb.wms.storeorder.application.port.in.command;

/**
 * PATCH /api/v1/orders/{orderId}/cancel 요청.
 *
 * @param storeOrderId 취소할 발주
 * @param reason       취소 사유 (500자 이하). 승인 이후(APPROVED·ASSIGNED·ON_HOLD) 취소는 필수, 승인 전(REQUESTED) 점주 취소는 선택
 * @param changedBy    처리 사용자 (인증 연동 전에는 요청 파라미터의 userId)
 */
public record StoreOrderCancelCommand(
        Long storeOrderId,
        String reason,
        Long changedBy
) {
}
