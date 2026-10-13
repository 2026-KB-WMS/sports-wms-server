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
     * 대상 사용자가 없으면 404 USER_NOT_FOUND, WAREHOUSE_MANAGER가 아니면 400, ACTIVE가 아닌 사용자(PENDING·INACTIVE)면 409.
     * 승인 전 사용자의 소속은 승인 요청(PATCH /users/{userId})에서 함께 지정한다.
     */
    WarehouseMemberView assignManager(WarehouseMemberAssignCommand command);

    /**
     * warehouseId·userId·keyword(사용자 이름·로그인 아이디 부분 일치)가 null이면 해당 조건을 무시하고 전체 배정을 조회한다.
     */
    List<WarehouseMemberView> getManagers(Long warehouseId, Long userId, String keyword);

    void releaseManager(Long warehouseMemberId);
}
