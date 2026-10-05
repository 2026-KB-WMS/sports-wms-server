package com.kb.wms.outbound.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 사유가 필요한 요청 바디: PATCH /allocations/{id}/release, PATCH /outbounds/{id}/cancel. */
public record OutboundReasonRequest(
        @NotBlank(message = "사유는 필수 값입니다.")
        @Size(max = 500, message = "사유는 최대 500자입니다.")
        String reason
) {
}
