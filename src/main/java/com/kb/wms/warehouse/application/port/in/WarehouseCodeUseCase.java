package com.kb.wms.warehouse.application.port.in;

import java.util.List;

import com.kb.wms.warehouse.application.port.in.result.CodeItem;

/**
 * 창고 도메인 고정 코드 목록 조회 유스케이스.
 * GET /api/v1/warehouses/management-types, GET /api/v1/warehouses/section-types
 */
public interface WarehouseCodeUseCase {

    List<CodeItem> getManagementTypes();

    List<CodeItem> getSectionTypes();
}
