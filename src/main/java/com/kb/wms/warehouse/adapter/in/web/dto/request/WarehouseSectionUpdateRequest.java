package com.kb.wms.warehouse.adapter.in.web.dto.request;

import java.math.BigDecimal;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.warehouse.application.port.in.command.WarehouseSectionUpdateCommand;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

/**
 * PATCH /api/v1/warehouses/sections/{sectionId} 요청 바디. 모든 필드는 선택이며, 값이 있는 필드만 수정한다.
 * warehouseId, parentSectionId, 운영 상태(isActive)는 이 API로 수정할 수 없다. 검증 애노테이션 대신
 * toCommand()에서 명시적으로 거부해, 명세대로 400 VALIDATION_ERROR로 응답한다.
 */
public record WarehouseSectionUpdateRequest(
        @Size(max = 50, message = "구역 코드는 최대 50자입니다.")
        String sectionCode,

        @Size(max = 100, message = "구역명은 최대 100자입니다.")
        String sectionName,

        String sectionType,

        @DecimalMin(value = "0", inclusive = true, message = "수용량은 0 이상이어야 합니다.")
        @Digits(integer = 11, fraction = 3, message = "수용량은 소수 3자리까지만 입력할 수 있습니다.")
        BigDecimal capacity,

        Long warehouseId,

        Long parentSectionId,

        Boolean isActive
) {

    public WarehouseSectionUpdateCommand toCommand() {
        if (warehouseId != null || parentSectionId != null || isActive != null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "warehouseId, parentSectionId, isActive는 이 API로 수정할 수 없습니다.");
        }
        return new WarehouseSectionUpdateCommand(sectionCode, sectionName, sectionType, capacity);
    }
}
