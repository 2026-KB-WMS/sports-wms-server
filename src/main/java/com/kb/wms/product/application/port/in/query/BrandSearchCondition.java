package com.kb.wms.product.application.port.in.query;

/**
 * GET /api/v1/products/brands 검색 조건. null인 조건은 무시한다.
 *
 * @param keyword  브랜드명 부분 일치
 * @param isActive 활성 상태 필터 (null이면 전체)
 */
public record BrandSearchCondition(
        String keyword,
        Boolean isActive
) {
}
