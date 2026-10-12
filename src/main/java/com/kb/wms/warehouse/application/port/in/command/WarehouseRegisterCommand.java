package com.kb.wms.warehouse.application.port.in.command;

import java.math.BigDecimal;

public record WarehouseRegisterCommand(
        String warehouseCode,
        String name,
        String address,
        String contactNumber,
        BigDecimal totalCapacity,
        BigDecimal latitude,
        BigDecimal longitude
) {

    public WarehouseRegisterCommand(String warehouseCode, String name, String address, String contactNumber,
                                    BigDecimal totalCapacity) {
        this(warehouseCode, name, address, contactNumber, totalCapacity, null, null);
    }
}
