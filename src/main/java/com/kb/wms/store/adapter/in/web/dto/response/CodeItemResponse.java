package com.kb.wms.store.adapter.in.web.dto.response;

import com.kb.wms.store.application.port.in.result.CodeItem;

/**
 * GET /api/v1/stores/management-types 응답 항목.
 */
public record CodeItemResponse(
        String code,
        String name
) {

    public static CodeItemResponse from(CodeItem item) {
        return new CodeItemResponse(item.code(), item.name());
    }
}
