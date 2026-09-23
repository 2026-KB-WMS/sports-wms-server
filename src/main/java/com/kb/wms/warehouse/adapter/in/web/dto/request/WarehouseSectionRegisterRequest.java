package com.kb.wms.warehouse.adapter.in.web.dto.request;

import java.math.BigDecimal;

import com.kb.wms.warehouse.application.port.in.command.WarehouseSectionRegisterCommand;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * POST /api/v1/warehouses/sections 요청 바디.
 */
public record WarehouseSectionRegisterRequest(
        @NotNull(message = "창고 ID는 필수 값입니다.")
        Long warehouseId,

        Long parentSectionId,

        @NotBlank(message = "구역 코드는 필수 값입니다.")
        @Size(max = 50, message = "구역 코드는 최대 50자입니다.")
        String sectionCode,

        @NotBlank(message = "구역명은 필수 값입니다.")
        @Size(max = 100, message = "구역명은 최대 100자입니다.")
        String sectionName,

        @NotBlank(message = "구역 유형은 필수 값입니다.")
        String sectionType,

        @DecimalMin(value = "0", inclusive = true, message = "수용량은 0 이상이어야 합니다.")
        @Digits(integer = 11, fraction = 3, message = "수용량은 소수 3자리까지만 입력할 수 있습니다.")
        BigDecimal capacity
) {

    public WarehouseSectionRegisterCommand toCommand() {
        return new WarehouseSectionRegisterCommand(
                warehouseId, parentSectionId, sectionCode, sectionName, sectionType, capacity);
    }
}
