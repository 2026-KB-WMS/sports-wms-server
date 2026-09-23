package com.kb.wms.warehouse.application.port.in;

import java.util.List;

import com.kb.wms.warehouse.application.port.in.command.WarehouseSectionRegisterCommand;
import com.kb.wms.warehouse.application.port.in.command.WarehouseSectionUpdateCommand;
import com.kb.wms.warehouse.domain.entity.WarehouseSection;

/**
 * 창고 구역 등록/조회/수정/비활성화 유스케이스.
 * POST, GET /api/v1/warehouses/sections, GET /api/v1/warehouses/{warehouseId}/sections
 */
public interface WarehouseSectionUseCase {

    WarehouseSection registerSection(WarehouseSectionRegisterCommand command);

    /**
     * warehouseId가 null이면 조건을 무시하고 전체 구역을 조회한다.
     */
    List<WarehouseSection> getSections(Long warehouseId);

    WarehouseSection getSection(Long sectionId);

    WarehouseSection updateSection(Long sectionId, WarehouseSectionUpdateCommand command);

    WarehouseSection deactivateSection(Long sectionId);
}
