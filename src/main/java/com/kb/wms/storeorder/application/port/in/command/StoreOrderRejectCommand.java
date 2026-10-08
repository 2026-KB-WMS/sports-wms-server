package com.kb.wms.storeorder.application.port.in.command;

/**
 * PATCH /api/v1/orders/{orderId}/reject 요청.
 *
 * @param storeOrderId 반려할 발주
 * @param reason       반려 사유 (필수, 500자 이하)
 * @param changedBy    처리 사용자 (토큰 사용자의 ID)
 */
public record StoreOrderRejectCommand(
        Long storeOrderId,
        String reason,
        Long changedBy
) {
}
