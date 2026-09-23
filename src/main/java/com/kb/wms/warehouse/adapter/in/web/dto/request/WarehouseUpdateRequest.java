package com.kb.wms.warehouse.adapter.in.web.dto.request;

import java.math.BigDecimal;

import com.kb.wms.warehouse.application.port.in.command.WarehouseUpdateCommand;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

/**
 * PATCH /api/v1/warehouses/{warehouseId} 요청 바디. 모든 필드는 선택이며, 값이 있는 필드만 수정한다.
 * warehouseCode와 운영 상태(isActive)는 포함하지 않는다.
 */
public record WarehouseUpdateRequest(
        @Size(max = 100, message = "창고명은 최대 100자입니다.")
        String warehouseName,

        @Size(max = 500, message = "창고 주소는 최대 500자입니다.")
        String address,

        @Size(max = 30, message = "연락처는 최대 30자입니다.")
        String contactNumber,

        @DecimalMin(value = "0", inclusive = true, message = "전체 수용량은 0 이상이어야 합니다.")
        BigDecimal totalCapacity
) {

    public WarehouseUpdateCommand toCommand() {
        return new WarehouseUpdateCommand(warehouseName, address, contactNumber, totalCapacity);
    }
}
