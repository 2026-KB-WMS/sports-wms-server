package com.kb.wms.outbound.adapter.in.web.dto.request;

import java.util.List;

import com.kb.wms.outbound.application.port.in.command.StockAllocateCommand;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * POST /api/v1/allocations 요청 바디. skuIds를 생략하면 발주 전체를 할당하고, 주면 그 SKU만 할당한다.
 */
public record StockAllocateRequest(
        @NotNull(message = "발주 ID는 필수 값입니다.")
        Long storeOrderId,

        @Size(min = 1, message = "SKU를 지정하려면 하나 이상 입력해주세요. 발주 전체를 할당하려면 생략합니다.")
        List<@NotNull(message = "SKU ID는 비워 둘 수 없습니다.") Long> skuIds
) {

    /** @param userId 처리 사용자. 토큰 사용자의 ID다. */
    public StockAllocateCommand toCommand(Long userId) {
        return new StockAllocateCommand(storeOrderId, userId, skuIds);
    }
}
