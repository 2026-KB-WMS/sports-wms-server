package com.kb.wms.outbound.adapter.in.web.dto.request;

import java.util.List;

import com.kb.wms.outbound.application.port.in.command.OutboundPickingCompleteCommand;
import com.kb.wms.outbound.application.port.in.command.OutboundPickingCompleteCommand.PickedLine;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * PATCH /api/v1/outbounds/{outboundId}/picking/complete 요청 바디. 항목 수·ID 일치와 중복 여부, 할당 수량 초과는
 * 서비스가 검증한다.
 */
public record OutboundPickingCompleteRequest(
        @NotEmpty(message = "피킹 항목(lines)은 필수 값입니다.")
        List<@Valid @NotNull(message = "피킹 항목은 null일 수 없습니다.") Line> lines
) {

    public record Line(
            @NotNull(message = "출고 항목 ID는 필수 값입니다.")
            Long outboundLineId,

            @NotNull(message = "피킹 수량은 필수 값입니다.")
            @PositiveOrZero(message = "피킹 수량은 0 이상이어야 합니다.")
            Long pickedQuantity
    ) {
    }

    public OutboundPickingCompleteCommand toCommand(Long outboundId, Long userId) {
        return new OutboundPickingCompleteCommand(outboundId,
                lines.stream().map(l -> new PickedLine(l.outboundLineId(), l.pickedQuantity())).toList(), userId);
    }
}
