package com.kb.wms.product.application.port.in.query;

/**
 * GET /api/v1/products 검색 조건. null인 조건은 무시한다.
 *
 * @param keyword  상품명·상품 코드 부분 일치
 * @param isActive 활성 상태 필터 (null이면 전체)
 */
public record ProductSearchCondition(
        Long brandId,
        Long categoryId,
        String keyword,
        Boolean isActive
) {
}
