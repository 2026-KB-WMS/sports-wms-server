package com.kb.wms.product.adapter.in.web.dto.response;

import com.kb.wms.product.application.port.in.result.CodeItem;

/**
 * GET /api/v1/products/skus/units 응답 항목.
 */
public record CodeItemResponse(
        String code,
        String name
) {

    public static CodeItemResponse from(CodeItem item) {
        return new CodeItemResponse(item.code(), item.name());
    }
}
