package com.kb.wms.warehouse.application.port.in.command;

import java.math.BigDecimal;

/**
 * POST /api/v1/warehouses 요청. 창고 코드는 받지 않고 주소의 시·도로 서버가 발급한다.
 */
public record WarehouseRegisterCommand(
        String name,
        String address,
        String contactNumber,
        BigDecimal totalCapacity,
        BigDecimal latitude,
        BigDecimal longitude
) {

    public WarehouseRegisterCommand(String name, String address, String contactNumber, BigDecimal totalCapacity) {
        this(name, address, contactNumber, totalCapacity, null, null);
    }
}
