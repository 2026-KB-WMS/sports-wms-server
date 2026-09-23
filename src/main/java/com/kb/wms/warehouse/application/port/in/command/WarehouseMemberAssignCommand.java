package com.kb.wms.warehouse.application.port.in.command;

public record WarehouseMemberAssignCommand(
        Long warehouseId,
        Long userId,
        String memberRole
) {
}
