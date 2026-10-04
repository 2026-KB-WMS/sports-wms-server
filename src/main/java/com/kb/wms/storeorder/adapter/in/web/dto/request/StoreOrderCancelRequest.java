package com.kb.wms.storeorder.adapter.in.web.dto.request;

import jakarta.validation.constraints.Size;

/**
 * PATCH /api/v1/orders/{orderId}/cancel 요청 바디.
 * 승인 이후(APPROVED·ASSIGNED·ON_HOLD) 취소는 사유가 필수이고, 승인 전(REQUESTED) 취소는 선택이다.
 * 상태에 따른 필수 여부는 서비스가 검증한다.
 */
public record StoreOrderCancelRequest(
        @Size(max = 500, message = "취소 사유는 최대 500자입니다.")
        String reason
) {
}
