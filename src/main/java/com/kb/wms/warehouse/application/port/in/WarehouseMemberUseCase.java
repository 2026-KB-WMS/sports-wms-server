package com.kb.wms.warehouse.application.port.in;

import java.util.List;

import com.kb.wms.warehouse.application.port.in.command.WarehouseMemberAssignCommand;
import com.kb.wms.warehouse.domain.entity.WarehouseMember;

/**
 * 창고 관리자 배정/조회/배정해제 유스케이스.
 * POST, GET /api/v1/warehouses/managers, DELETE /api/v1/warehouses/managers/{warehouseMemberId}
 */
public interface WarehouseMemberUseCase {

    WarehouseMember assignManager(WarehouseMemberAssignCommand command);

    /**
     * warehouseId가 null이면 조건을 무시하고 전체 배정을 조회한다.
     */
    List<WarehouseMember> getManagers(Long warehouseId);

    void releaseManager(Long warehouseMemberId);
}
