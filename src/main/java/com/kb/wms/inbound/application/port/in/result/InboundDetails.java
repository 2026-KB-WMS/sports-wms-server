package com.kb.wms.inbound.application.port.in.result;

import java.util.List;

import com.kb.wms.inbound.domain.enums.InboundStatus;

/**
 * 입고 상세 (GET /api/v1/inbounds/{inboundId}/details): 입고 식별 정보 + 검수 항목. 검수 전(ARRIVED)에는 items가 비어 있다.
 */
public record InboundDetails(
        Long inboundId,
        String inboundNo,
        InboundStatus status,
        List<InboundLineView> items
) {
}
