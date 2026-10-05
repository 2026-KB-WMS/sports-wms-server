package com.kb.wms.outbound.adapter.in.web.dto.request;

import com.kb.wms.outbound.application.port.in.command.OutboundCreateCommand;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** POST /api/v1/outbounds 요청 바디. */
public record OutboundCreateRequest(
        @NotNull(message = "발주 ID는 필수 값입니다.")
        Long storeOrderId,

        @Size(max = 500, message = "비고는 최대 500자입니다.")
        String note
) {

    public OutboundCreateCommand toCommand(Long userId) {
        return new OutboundCreateCommand(storeOrderId, note, userId);
    }
}
