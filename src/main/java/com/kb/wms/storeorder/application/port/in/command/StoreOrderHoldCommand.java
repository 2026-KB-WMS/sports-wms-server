package com.kb.wms.storeorder.application.port.in.command;

/**
 * PATCH /api/v1/orders/{orderId}/hold 요청.
 *
 * @param storeOrderId 출고를 보류할 발주
 * @param reason       보류 사유 (필수, 500자 이하)
 * @param changedBy    처리 사용자 (인증 연동 전에는 요청 파라미터의 userId)
 */
public record StoreOrderHoldCommand(
        Long storeOrderId,
        String reason,
        Long changedBy
) {
}
