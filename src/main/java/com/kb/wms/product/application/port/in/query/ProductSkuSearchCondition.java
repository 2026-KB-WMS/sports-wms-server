package com.kb.wms.product.application.port.in.query;

/**
 * GET /api/v1/products/skus 검색 조건. null인 조건은 무시한다.
 *
 * @param brandId    SKU가 속한 상품의 브랜드
 * @param categoryId SKU가 속한 상품의 카테고리
 * @param keyword    SKU 코드·바코드·SKU명 부분 일치
 * @param isActive   SKU 활성 상태 필터 (null이면 전체)
 */
public record ProductSkuSearchCondition(
        Long productId,
        Long brandId,
        Long categoryId,
        String keyword,
        Boolean isActive
) {
}
