package com.kb.wms.inbound.adapter.in.web.dto.request;

import com.kb.wms.inbound.application.port.in.command.InboundCancelCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * PATCH /api/v1/inbounds/{inboundId}/cancel 요청 바디. 취소 사유는 필수다.
 */
public record InboundCancelRequest(
        @NotBlank(message = "취소 사유는 필수 값입니다.")
        @Size(max = 500, message = "취소 사유는 최대 500자입니다.")
        String reason
) {

    public InboundCancelCommand toCommand(Long userId) {
        return new InboundCancelCommand(reason, userId);
    }
}
