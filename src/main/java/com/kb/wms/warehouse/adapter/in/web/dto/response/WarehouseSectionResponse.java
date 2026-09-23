package com.kb.wms.warehouse.adapter.in.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.kb.wms.warehouse.domain.entity.WarehouseSection;

/**
 * 창고 구역 등록/조회/수정/비활성화 응답. availableCapacity는 capacity - currentCapacity로 계산해 저장하지 않는다.
 * warehouseName·parentSectionCode는 명세상 조회 응답(전체/상세)에 필요한 필드로, 컨트롤러에서 조회해 채운다.
 */
public record WarehouseSectionResponse(
        Long sectionId,
        Long warehouseId,
        String warehouseName,
        Long parentSectionId,
        String parentSectionCode,
        String sectionCode,
        String sectionName,
        String sectionType,
        BigDecimal capacity,
        BigDecimal currentCapacity,
        BigDecimal availableCapacity,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static WarehouseSectionResponse from(WarehouseSection section, String warehouseName, String parentSectionCode) {
        return new WarehouseSectionResponse(
                section.getSectionId(),
                section.getWarehouseId(),
                warehouseName,
                section.getParentSectionId(),
                parentSectionCode,
                section.getSectionCode(),
                section.getName(),
                section.getSectionType(),
                section.getCapacity(),
                section.getCurrentCapacity(),
                section.availableCapacity(),
                section.isActive(),
                section.getCreatedAt(),
                section.getUpdatedAt());
    }
}
