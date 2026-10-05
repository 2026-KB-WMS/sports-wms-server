package com.kb.wms.outbound.application.port.in.command;

import java.util.List;

/**
 * 피킹 완료 요청(PATCH /api/v1/outbounds/{outboundId}/picking/complete).
 *
 * @param lines 이 출고의 모든 항목별 피킹 수량(중복 불가)
 */
public record OutboundPickingCompleteCommand(
        Long outboundId,
        List<PickedLine> lines,
        Long userId
) {

    /**
     * @param outboundLineId 출고 항목 ID
     * @param pickedQuantity 실제 피킹 수량(0 이상, 항목 할당 수량 이하)
     */
    public record PickedLine(Long outboundLineId, Long pickedQuantity) {
    }
}
