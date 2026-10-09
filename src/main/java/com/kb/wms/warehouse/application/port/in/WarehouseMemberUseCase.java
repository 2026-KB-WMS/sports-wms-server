package com.kb.wms.warehouse.application.port.in;

import java.util.List;

import com.kb.wms.warehouse.application.port.in.command.WarehouseMemberAssignCommand;
import com.kb.wms.warehouse.application.port.in.result.WarehouseMemberView;

/**
 * 창고 관리자 배정/조회/배정해제 유스케이스.
 * POST, GET /api/v1/warehouses/managers, DELETE /api/v1/warehouses/managers/{warehouseMemberId}
 */
public interface WarehouseMemberUseCase {

    /**
     * 대상 사용자가 없으면 404 USER_NOT_FOUND, WAREHOUSE_MANAGER가 아니면 400, INACTIVE 사용자면 409.
     * PENDING 사용자는 배정할 수 있다(소속 배정 후 승인).
     */
    WarehouseMemberView assignManager(WarehouseMemberAssignCommand command);

    /**
     * warehouseId·userId·keyword(사용자 이름·로그인 아이디 부분 일치)가 null이면 해당 조건을 무시하고 전체 배정을 조회한다.
     */
    List<WarehouseMemberView> getManagers(Long warehouseId, Long userId, String keyword);

    void releaseManager(Long warehouseMemberId);
}
