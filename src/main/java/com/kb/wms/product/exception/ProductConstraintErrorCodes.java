package com.kb.wms.product.exception;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.kb.wms.common.exception.ConstraintErrorCodeProvider;
import com.kb.wms.common.exception.DomainErrorCode;

/**
 * 상품 도메인 유니크 제약 조건 → 오류 코드 (V2__create_product_tables.sql 기준).
 */
@Component
public class ProductConstraintErrorCodes implements ConstraintErrorCodeProvider {

    @Override
    public Map<String, DomainErrorCode> constraintErrorCodes() {
        return Map.of(
                "uk_brand_name", ProductErrorCode.DUPLICATE_BRAND_NAME,
                "uk_category_code", ProductErrorCode.DUPLICATE_CATEGORY_CODE,
                "uk_product_code", ProductErrorCode.DUPLICATE_PRODUCT_CODE,
                "uk_product_sku_code", ProductErrorCode.DUPLICATE_SKU_CODE,
                "uk_product_sku_barcode", ProductErrorCode.DUPLICATE_BARCODE,
                "uk_option_group_name", ProductErrorCode.DUPLICATE_OPTION_GROUP_NAME,
                "uk_option_value_group_value", ProductErrorCode.DUPLICATE_OPTION_VALUE);
    }
}
