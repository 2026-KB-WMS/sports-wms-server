package com.kb.wms.storeorder.application.port.in.result;

import java.time.LocalDateTime;

/**
 * 지점 발주 상태 이력 한 건 (GET /api/v1/orders/{orderId}/details 의 statusHistory[]).
 * 처리자 이름은 회원 도메인이 생기면 웹 어댑터에서 changedBy로 채운다.
 *
 * @param fromStatus 최초 등록이면 null
 */
public record StoreOrderStatusHistoryView(
        String fromStatus,
        String toStatus,
        String reason,
        Long changedBy,
        LocalDateTime changedAt
) {
}
