package com.kb.wms.warehouse.adapter.in.web.dto.request;

import java.math.BigDecimal;

import com.kb.wms.warehouse.application.port.in.command.WarehouseRegisterCommand;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * POST /api/v1/warehouses 요청 바디.
 */
public record WarehouseRegisterRequest(
        @NotBlank(message = "창고 코드는 필수 값입니다.")
        @Size(max = 30, message = "창고 코드는 최대 30자입니다.")
        String warehouseCode,

        @NotBlank(message = "창고명은 필수 값입니다.")
        @Size(max = 100, message = "창고명은 최대 100자입니다.")
        String warehouseName,

        @NotBlank(message = "창고 주소는 필수 값입니다.")
        @Size(max = 500, message = "창고 주소는 최대 500자입니다.")
        String address,

        @Size(max = 30, message = "연락처는 최대 30자입니다.")
        String contactNumber,

        @DecimalMin(value = "0", inclusive = true, message = "전체 수용량은 0 이상이어야 합니다.")
        BigDecimal totalCapacity
) {

    public WarehouseRegisterCommand toCommand() {
        return new WarehouseRegisterCommand(warehouseCode, warehouseName, address, contactNumber, totalCapacity);
    }
}
