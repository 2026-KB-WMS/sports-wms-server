package com.kb.wms.product.application.port.in.query;

/**
 * GET /api/v1/products/categories 검색 조건. null인 조건은 무시한다.
 *
 * @param parentCategoryId 이 카테고리의 직속 하위 카테고리만
 * @param depth            계층 깊이 (1 = 최상위)
 * @param keyword          카테고리명·카테고리 코드 부분 일치
 * @param isActive         활성 상태 필터 (null이면 전체)
 */
public record CategorySearchCondition(
        Long parentCategoryId,
        Integer depth,
        String keyword,
        Boolean isActive
) {
}
