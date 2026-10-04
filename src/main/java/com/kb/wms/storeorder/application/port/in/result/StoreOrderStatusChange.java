package com.kb.wms.storeorder.application.port.in.result;

import java.time.LocalDateTime;

import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

/**
 * 승인·반려 처리 결과 (PATCH /api/v1/orders/{orderId}/approve, /reject 응답).
 *
 * @param statusReason 반려 사유. 승인은 사유가 없어 null이다.
 * @param updatedAt    상태 변경 반영 후 수정 일시
 */
public record StoreOrderStatusChange(
        Long storeOrderId,
        String orderNo,
        StoreOrderStatus status,
        String statusReason,
        LocalDateTime updatedAt
) {
}
