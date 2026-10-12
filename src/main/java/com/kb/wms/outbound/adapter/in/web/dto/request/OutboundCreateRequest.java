package com.kb.wms.outbound.adapter.in.web.dto.request;

import java.util.List;

import com.kb.wms.outbound.application.port.in.command.OutboundCreateCommand;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * POST /api/v1/outbounds 요청 바디. skuIds를 생략하면 연결되지 않은 할당 전부를 묶고, 주면 그 SKU의 할당만 묶는다.
 */
public record OutboundCreateRequest(
        @NotNull(message = "발주 ID는 필수 값입니다.")
        Long storeOrderId,

        @Size(max = 500, message = "비고는 최대 500자입니다.")
        String note,

        @Size(min = 1, message = "SKU를 지정하려면 하나 이상 입력해주세요. 전체를 묶으려면 생략합니다.")
        List<@NotNull(message = "SKU ID는 비워 둘 수 없습니다.") Long> skuIds
) {

    public OutboundCreateCommand toCommand(Long userId) {
        return new OutboundCreateCommand(storeOrderId, note, userId, skuIds);
    }
}
