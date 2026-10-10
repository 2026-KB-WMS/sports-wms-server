package com.kb.wms.warehouse.application.port.in;

import java.util.List;

import com.kb.wms.common.security.AuthenticatedUser;
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

    /** 사용자 요청용 단건 조회. 구역이 속한 창고가 담당 창고(본사는 전체)가 아니면 403 FORBIDDEN. */
    WarehouseSection getSection(Long sectionId, AuthenticatedUser actor);

    /** 사용자 요청용 목록 조회. 지정한 창고가 담당 창고(본사는 전체)가 아니면 403 FORBIDDEN. 창고를 지정하지 않으면 본사만 가능하다. */
    List<WarehouseSection> getSections(WarehouseSectionSearchCondition condition, AuthenticatedUser actor);

    WarehouseSection updateSection(Long sectionId, WarehouseSectionUpdateCommand command);

    WarehouseSection deactivateSection(Long sectionId);

    /**
     * 잘못 등록한 구역을 삭제한다. 재고(보유·할당)가 있으면 409 SECTION_HAS_INVENTORY, 하위 구역이 있으면(비활성 포함)
     * 409 SECTION_HAS_CHILDREN, 재고 로트나 입고 검수 항목이 참조하면 409 SECTION_IN_USE. 이력이 남는 구역은 비활성화를 쓴다.
     */
    void deleteSection(Long sectionId);

    /**
     * 비활성 구역을 다시 활성화한다. 이미 활성이면 409 CONFLICT, 창고나 상위 구역이 비활성이면 409 CONFLICT.
     * 비활성 창고 아래에 활성 구역이 생기지 않게 상위부터 활성화해야 한다.
     */
    WarehouseSection activateSection(Long sectionId);
}
