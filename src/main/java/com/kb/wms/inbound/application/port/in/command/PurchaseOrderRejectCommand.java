package com.kb.wms.inbound.application.port.in.command;

/**
 * PATCH /api/v1/purchase-orders/{purchaseOrderId}/reject 요청.
 *
 * @param reason 반려 사유 (필수, 최대 500자)
 * @param userId 반려 처리 사용자 (토큰 사용자의 ID). 상태 이력의 처리자로 기록한다
 */
public record PurchaseOrderRejectCommand(
        String reason,
        Long userId
) {
}
