package com.kb.wms.warehouse.application.port.in.result;

/**
 * GET /warehouses/management-types, GET /warehouses/section-types 등 고정 코드 목록 조회 응답의 항목.
 */
public record CodeItem(
        String code,
        String name
) {
}
