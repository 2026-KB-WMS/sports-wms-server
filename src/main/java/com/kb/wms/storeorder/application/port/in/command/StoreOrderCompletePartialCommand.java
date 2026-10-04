package com.kb.wms.storeorder.application.port.in.command;

/**
 * PATCH /api/v1/orders/{orderId}/complete-partial 요청.
 *
 * @param storeOrderId 부분 출고로 종결할 발주
 * @param reason       종결 사유 (필수, 500자 이하. 재고 불가·공급 중단 등)
 * @param changedBy    처리 사용자 (인증 연동 전에는 요청 파라미터의 userId)
 */
public record StoreOrderCompletePartialCommand(
        Long storeOrderId,
        String reason,
        Long changedBy
) {
}
