package com.kb.wms.outbound.adapter.in.web.dto.request;

import com.kb.wms.outbound.application.port.in.command.StockAllocateCommand;

import jakarta.validation.constraints.NotNull;

/** POST /api/v1/allocations 요청 바디. */
public record StockAllocateRequest(
        @NotNull(message = "발주 ID는 필수 값입니다.")
        Long storeOrderId
) {

    /** @param userId 처리 사용자. 인증 연동 전에는 쿼리 파라미터로 받는다. */
    public StockAllocateCommand toCommand(Long userId) {
        return new StockAllocateCommand(storeOrderId, userId);
    }
}
