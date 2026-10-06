package com.kb.wms.storeorder.application.port.in.result;

import java.time.LocalDateTime;

/**
 * 지점 발주 상태 이력 한 건 (GET /api/v1/orders/{orderId}/details 의 statusHistory[]).
 * 처리자 이름(changedByName)은 아직 채우지 않는다. 공통 상태 이력 패키지가 auth 엔티티를 몰라야 해서,
 * 이력 패키지를 분리할 때 함께 채운다.
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
