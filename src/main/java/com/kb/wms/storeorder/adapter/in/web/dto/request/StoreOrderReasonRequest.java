package com.kb.wms.storeorder.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 사유가 필수인 상태 변경 요청 바디.
 * PATCH /api/v1/orders/{orderId}/reject, hold, resume, complete-partial 이 함께 쓴다.
 */
public record StoreOrderReasonRequest(
        @NotBlank(message = "사유는 필수 값입니다.")
        @Size(max = 500, message = "사유는 최대 500자입니다.")
        String reason
) {
}
