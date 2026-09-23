package com.kb.wms.warehouse.adapter.in.web.dto.response;

import com.kb.wms.warehouse.application.port.in.result.CodeItem;

/**
 * GET /api/v1/warehouses/management-types, GET /api/v1/warehouses/section-types 응답 항목.
 */
public record CodeItemResponse(
        String code,
        String name
) {

    public static CodeItemResponse from(CodeItem item) {
        return new CodeItemResponse(item.code(), item.name());
    }
}
