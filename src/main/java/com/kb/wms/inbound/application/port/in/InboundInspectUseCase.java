package com.kb.wms.inbound.application.port.in;

import com.kb.wms.inbound.application.port.in.command.InboundInspectCommand;
import com.kb.wms.inbound.domain.entity.Inbound;

/**
 * 입고 검수 유스케이스. PATCH /api/v1/inbounds/{inboundId}/inspect
 */
public interface InboundInspectUseCase {

    /**
     * 검수 항목 전체를 요청 내용으로 교체한다. 처음 호출하면 ARRIVED → INSPECTING으로 바뀌고,
     * 검수 중 재호출은 상태를 유지한 채 항목만 교체한다. 재고·구역 사용량·발주 항목은 바꾸지 않는다.
     */
    Inbound inspectInbound(Long inboundId, InboundInspectCommand command);
}
