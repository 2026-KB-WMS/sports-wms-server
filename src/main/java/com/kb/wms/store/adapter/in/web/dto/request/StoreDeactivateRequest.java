package com.kb.wms.store.adapter.in.web.dto.request;

import jakarta.validation.constraints.Size;

/**
 * PATCH /api/v1/stores/{storeId}/deactivate 요청 바디. 바디와 reason 모두 선택이며,
 * 값이 있으면 StatusHistory.reason에 저장한다.
 */
public record StoreDeactivateRequest(
        @Size(max = 500, message = "사유는 최대 500자입니다.")
        String reason
) {
}
