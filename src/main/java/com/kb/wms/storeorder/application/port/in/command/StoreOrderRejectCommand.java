package com.kb.wms.storeorder.application.port.in.command;

/**
 * PATCH /api/v1/orders/{orderId}/reject 요청.
 *
 * @param storeOrderId 반려할 발주
 * @param reason       반려 사유 (필수, 500자 이하)
 * @param changedBy    처리 사용자 (인증 연동 전에는 요청 파라미터의 userId)
 */
public record StoreOrderRejectCommand(
        Long storeOrderId,
        String reason,
        Long changedBy
) {
}
