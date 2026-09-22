package com.kb.wms.warehouse.application.port.in.command;

import java.math.BigDecimal;

public record WarehouseRegisterCommand(
        String warehouseCode,
        String name,
        String address,
        String contactNumber,
        BigDecimal totalCapacity
) {
}
