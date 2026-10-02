package com.kb.wms.inbound.application.port.in.result;

import java.math.BigDecimal;

/**
 * 입고 검수에서 고를 수 있는 구역 후보 (assignable-sections, defect-sections 공용).
 * availableCapacity = capacity - currentCapacity 이며 후보는 항상 0보다 크다.
 */
public record SectionCandidate(
        Long sectionId,
        Long parentSectionId,
        String sectionCode,
        String sectionName,
        String sectionType,
        BigDecimal capacity,
        BigDecimal currentCapacity,
        BigDecimal availableCapacity
) {
}
