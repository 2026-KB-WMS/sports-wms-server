package com.kb.wms.warehouse.application.port.in.command;

import java.math.BigDecimal;

/**
 * PATCH /api/v1/warehouses/{warehouseId} 요청. 모든 필드는 선택이며, 값이 있는 필드만 부분 수정한다.
 * warehouseCode와 운영 상태(isActive)는 이 커맨드에 포함하지 않는다. latitude·longitude는 쌍으로만 오며 생략하면 기존 좌표를 유지한다.
 */
public record WarehouseUpdateCommand(
        String name,
        String address,
        String contactNumber,
        BigDecimal totalCapacity,
        BigDecimal latitude,
        BigDecimal longitude
) {

    public WarehouseUpdateCommand(String name, String address, String contactNumber, BigDecimal totalCapacity) {
        this(name, address, contactNumber, totalCapacity, null, null);
    }

    public boolean hasNoChanges() {
        return name == null && address == null && contactNumber == null && totalCapacity == null
                && latitude == null && longitude == null;
    }
}
