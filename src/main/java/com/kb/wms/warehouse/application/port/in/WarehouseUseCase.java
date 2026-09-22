package com.kb.wms.warehouse.application.port.in;

import java.util.List;

import com.kb.wms.warehouse.application.port.in.command.WarehouseRegisterCommand;
import com.kb.wms.warehouse.application.port.in.command.WarehouseUpdateCommand;
import com.kb.wms.warehouse.domain.entity.Warehouse;

/**
 * 창고 등록/조회/수정/비활성화 유스케이스.
 * POST, GET, PATCH /api/v1/warehouses, GET /api/v1/warehouses/my
 */
public interface WarehouseUseCase {

    Warehouse registerWarehouse(WarehouseRegisterCommand command);

    List<Warehouse> getWarehouses();

    Warehouse getWarehouse(Long warehouseId);

    Warehouse updateWarehouse(Long warehouseId, WarehouseUpdateCommand command);

    Warehouse deactivateWarehouse(Long warehouseId);

    /**
     * 로그인한 창고 관리자가 배정된 창고를 조회한다.
     */
    Warehouse getMyWarehouse(Long userId);
}
