package com.kb.wms.inbound.application.port.in.query;

import java.math.BigDecimal;

/**
 * GET /api/v1/inbounds/{inboundId}/assignable-sections, .../defect-sections 검색 조건.
 *
 * @param requiredQuantity 이 수량 이상 가용 용량(capacity - currentCapacity)이 있는 구역만 조회 (선택, 0 이상)
 * @param keyword          구역명·구역 코드 부분 일치 (선택)
 */
public record SectionCandidateCondition(
        BigDecimal requiredQuantity,
        String keyword
) {
}
