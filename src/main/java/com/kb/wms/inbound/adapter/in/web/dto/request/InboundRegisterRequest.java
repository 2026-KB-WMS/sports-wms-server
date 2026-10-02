package com.kb.wms.inbound.adapter.in.web.dto.request;

import java.time.LocalDateTime;

import com.kb.wms.inbound.application.port.in.command.InboundRegisterCommand;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * POST /api/v1/inbounds 요청 바디. 입고 대상 창고는 발주의 창고를 따르므로 받지 않는다.
 */
public record InboundRegisterRequest(
        @NotNull(message = "발주 ID는 필수 값입니다.")
        Long purchaseOrderId,

        LocalDateTime arrivedAt,

        @Size(max = 1000, message = "비고는 최대 1000자입니다.")
        String note
) {

    public InboundRegisterCommand toCommand() {
        return new InboundRegisterCommand(purchaseOrderId, arrivedAt, note);
    }
}
