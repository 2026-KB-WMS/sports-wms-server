package com.kb.wms.storeorder.application.port.in.command;

/**
 * PATCH /api/v1/orders/{orderId}/resume 요청.
 *
 * @param storeOrderId 출고를 재개할 발주
 * @param reason       재개 사유 (필수, 500자 이하)
 * @param changedBy    처리 사용자 (토큰 사용자의 ID)
 */
public record StoreOrderResumeCommand(
        Long storeOrderId,
        String reason,
        Long changedBy
) {
}
