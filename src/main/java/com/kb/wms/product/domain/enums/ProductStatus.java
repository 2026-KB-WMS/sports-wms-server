package com.kb.wms.product.domain.enums;

/**
 * 상품 도메인 공통 사용 여부 상태 (Brand, Category, Product, ProductSku, OptionValue).
 */
public enum ProductStatus {
    ACTIVE,
    INACTIVE;

    /** isActive 필터 값을 상태로 바꾼다. null이면 조건 없음(null). */
    public static ProductStatus fromActiveFlag(Boolean isActive) {
        if (isActive == null) {
            return null;
        }
        return isActive ? ACTIVE : INACTIVE;
    }
}
