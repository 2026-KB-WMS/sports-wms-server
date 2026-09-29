package com.kb.wms.store.application.port.in.result;

/**
 * GET /stores/management-types 등 고정 코드 목록 조회 응답의 항목.
 */
public record CodeItem(
        String code,
        String name
) {
}
