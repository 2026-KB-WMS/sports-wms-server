package com.kb.wms.warehouse.application.port.in.command;

import java.math.BigDecimal;

/**
 * PATCH /api/v1/warehouses/sections/{sectionId} 요청. 모든 필드는 선택이며, 값이 있는 필드만 부분 수정한다.
 * warehouseId, parentSectionId, 운영 상태(isActive)는 이 커맨드에 포함하지 않는다.
 */
public record WarehouseSectionUpdateCommand(
        String sectionCode,
        String name,
        String sectionType,
        BigDecimal capacity
) {

    public boolean hasNoChanges() {
        return sectionCode == null && name == null && sectionType == null && capacity == null;
    }
}
