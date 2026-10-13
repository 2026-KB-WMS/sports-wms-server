package com.kb.wms.product.application.port.in.result;

/**
 * GET /products/skus/units 등 고정 코드 목록 조회 응답의 항목.
 */
public record CodeItem(
        String code,
        String name
) {
}
