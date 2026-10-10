package com.kb.wms.storeorder.application.port.in.result;

import java.time.LocalDateTime;

/**
 * 지점 발주 상태 이력 한 건 (GET /api/v1/orders/{orderId}/details 의 statusHistory[]).
 *
 * @param fromStatus    최초 등록이면 null
 * @param changedByName 처리자 사용자가 없으면 null
 */
public record StoreOrderStatusHistoryView(
        String fromStatus,
        String toStatus,
        String reason,
        Long changedBy,
        String changedByName,
        LocalDateTime changedAt
) {
}
