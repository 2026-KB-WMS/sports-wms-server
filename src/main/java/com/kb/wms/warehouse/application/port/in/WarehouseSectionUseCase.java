package com.kb.wms.warehouse.application.port.in;

import java.util.List;

import com.kb.wms.warehouse.application.port.in.command.WarehouseSectionRegisterCommand;
import com.kb.wms.warehouse.application.port.in.command.WarehouseSectionUpdateCommand;
import com.kb.wms.warehouse.application.port.in.query.WarehouseSectionSearchCondition;
import com.kb.wms.warehouse.domain.entity.WarehouseSection;

/**
 * 창고 구역 등록/조회/수정/비활성화 유스케이스.
 * POST, GET /api/v1/warehouses/sections, GET /api/v1/warehouses/{warehouseId}/sections
 */
public interface WarehouseSectionUseCase {

    WarehouseSection registerSection(WarehouseSectionRegisterCommand command);

    /**
     * 구역 목록 조회. 조건이 null이면 무시한다.
     * 필터의 창고·상위 구역이 없으면 WAREHOUSE_NOT_FOUND·PARENT_SECTION_NOT_FOUND, 허용되지 않은 구역 유형은 VALIDATION_ERROR.
     */
    List<WarehouseSection> getSections(WarehouseSectionSearchCondition condition);

    WarehouseSection getSection(Long sectionId);

    WarehouseSection updateSection(Long sectionId, WarehouseSectionUpdateCommand command);

    WarehouseSection deactivateSection(Long sectionId);
}
