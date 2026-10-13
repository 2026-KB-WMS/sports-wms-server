package com.kb.wms.product.domain.enums;

/**
 * SKU 수량 단위(ProductSku.unit)의 서버 허용 목록. 등록 요청 검증과
 * GET /api/v1/products/skus/units 응답에 쓴다(ADR-024).
 */
public enum SkuUnit {

    EA("개"),
    BOX("박스"),
    SET("세트");

    private final String description;

    SkuUnit(String description) {
        this.description = description;
    }

    public String getCode() {
        return name();
    }

    public String getDescription() {
        return description;
    }

    public static boolean isValidCode(String code) {
        for (SkuUnit unit : values()) {
            if (unit.name().equals(code)) {
                return true;
            }
        }
        return false;
    }
}
