package com.kb.wms.warehouse.application.port.in.query;

/**
 * GET /api/v1/warehouses/sections, /warehouses/{warehouseId}/sections 검색 조건. null인 조건은 무시한다.
 *
 * @param parentSectionId 이 구역의 직속 하위 구역만
 * @param sectionType     구역 유형 코드 (GET /warehouses/section-types의 code)
 * @param keyword         구역명·구역 코드 부분 일치
 * @param isActive        사용 여부 필터 (null이면 전체)
 */
public record WarehouseSectionSearchCondition(
        Long warehouseId,
        Long parentSectionId,
        String sectionType,
        String keyword,
        Boolean isActive
) {
}
