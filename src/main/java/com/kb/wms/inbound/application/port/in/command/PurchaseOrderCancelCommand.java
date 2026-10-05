package com.kb.wms.inbound.application.port.in.command;

/**
 * PATCH /api/v1/purchase-orders/{purchaseOrderId}/cancel 요청.
 *
 * @param reason 취소 사유 (최대 500자). CONFIRMED 발주를 본사 관리자가 취소할 때 필수, REQUESTED 취소 때는 선택
 * @param userId 취소 처리 사용자 (인증 연동 전 임시로 쿼리 파라미터로 받는다). 상태 이력의 처리자로 기록한다
 */
public record PurchaseOrderCancelCommand(
        String reason,
        Long userId
) {
}
