package com.kb.wms.storeorder.application.port.in.result;

import java.time.LocalDateTime;

import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

/**
 * 취소 처리 결과 (PATCH /api/v1/orders/{orderId}/cancel 응답).
 * 출고 도메인 연동 전에는 releasedAllocationCount·canceledOutboundCount가 항상 0이다.
 *
 * @param statusReason            취소 사유. 승인 전 점주 취소는 사유를 생략할 수 있어 null일 수 있다.
 * @param releasedAllocationCount 해제(RELEASED)한 재고 할당 수
 * @param canceledOutboundCount   취소(CANCELED)한 READY 출고 수
 */
public record StoreOrderCancelResult(
        Long storeOrderId,
        String orderNo,
        StoreOrderStatus status,
        String statusReason,
        int releasedAllocationCount,
        int canceledOutboundCount,
        LocalDateTime updatedAt
) {
}
