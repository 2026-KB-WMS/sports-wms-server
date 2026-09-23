package com.kb.wms.warehouse.application.port.in;

import java.util.List;

import com.kb.wms.warehouse.application.port.in.command.WarehouseRegisterCommand;
import com.kb.wms.warehouse.application.port.in.command.WarehouseUpdateCommand;
import com.kb.wms.warehouse.application.port.in.result.WarehouseMembershipSummary;
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
     * 로그인한 창고 관리자가 배정된 창고를 모두 조회한다.
     * 한 사용자가 여러 창고에 배정될 수 있어 목록으로 반환하며, 창고 정보와 배정(WarehouseMember) 정보를 함께 담는다.
     */
    List<WarehouseMembershipSummary> getMyWarehouses(Long userId);
}
