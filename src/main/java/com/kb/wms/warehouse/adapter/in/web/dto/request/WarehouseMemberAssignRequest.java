package com.kb.wms.warehouse.adapter.in.web.dto.request;

import com.kb.wms.warehouse.application.port.in.command.WarehouseMemberAssignCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * POST /api/v1/warehouses/managers 요청 바디.
 */
public record WarehouseMemberAssignRequest(
        @NotNull(message = "창고 ID는 필수 값입니다.")
        Long warehouseId,

        @NotNull(message = "사용자 ID는 필수 값입니다.")
        Long userId,

        @NotBlank(message = "담당 역할은 필수 값입니다.")
        String memberRole
) {

    public WarehouseMemberAssignCommand toCommand() {
        return new WarehouseMemberAssignCommand(warehouseId, userId, memberRole);
    }
}
