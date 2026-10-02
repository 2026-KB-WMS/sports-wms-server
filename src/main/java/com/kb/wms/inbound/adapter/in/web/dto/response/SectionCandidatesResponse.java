package com.kb.wms.inbound.adapter.in.web.dto.response;

import java.math.BigDecimal;
import java.util.List;

import com.kb.wms.inbound.application.port.in.result.SectionCandidate;

/**
 * GET /api/v1/inbounds/{inboundId}/assignable-sections, /defect-sections (검수 구역 후보) 공용 응답.
 */
public record SectionCandidatesResponse(
        Long inboundId,
        Long warehouseId,
        List<Item> items
) {

    public record Item(
            Long sectionId,
            Long parentSectionId,
            String sectionCode,
            String sectionName,
            String sectionType,
            BigDecimal capacity,
            BigDecimal currentCapacity,
            BigDecimal availableCapacity
    ) {

        static Item from(SectionCandidate candidate) {
            return new Item(
                    candidate.sectionId(), candidate.parentSectionId(), candidate.sectionCode(),
                    candidate.sectionName(), candidate.sectionType(), candidate.capacity(),
                    candidate.currentCapacity(), candidate.availableCapacity());
        }
    }

    public static SectionCandidatesResponse of(Long inboundId, Long warehouseId, List<SectionCandidate> candidates) {
        return new SectionCandidatesResponse(
                inboundId, warehouseId, candidates.stream().map(Item::from).toList());
    }
}
