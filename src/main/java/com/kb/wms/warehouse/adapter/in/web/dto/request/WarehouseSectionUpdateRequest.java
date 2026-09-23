package com.kb.wms.warehouse.adapter.in.web.dto.request;

import java.math.BigDecimal;

import com.kb.wms.warehouse.application.port.in.command.WarehouseSectionUpdateCommand;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

/**
 * PATCH /api/v1/warehouses/sections/{sectionId} 요청 바디. 모든 필드는 선택이며, 값이 있는 필드만 수정한다.
 * warehouseId, parentSectionId, 운영 상태(isActive)는 포함하지 않는다.
 */
public record WarehouseSectionUpdateRequest(
        @Size(max = 50, message = "구역 코드는 최대 50자입니다.")
        String sectionCode,

        @Size(max = 100, message = "구역명은 최대 100자입니다.")
        String sectionName,

        String sectionType,

        @DecimalMin(value = "0", inclusive = true, message = "수용량은 0 이상이어야 합니다.")
        BigDecimal capacity
) {

    public WarehouseSectionUpdateCommand toCommand() {
        return new WarehouseSectionUpdateCommand(sectionCode, sectionName, sectionType, capacity);
    }
}
