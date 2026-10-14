package com.kb.wms.auth.adapter.out.warehouse;

import org.springframework.stereotype.Component;

import com.kb.wms.auth.application.port.out.WarehouseManagerAssignPort;
import com.kb.wms.warehouse.application.port.in.WarehouseMemberUseCase;
import com.kb.wms.warehouse.application.port.in.command.WarehouseMemberAssignCommand;

import lombok.RequiredArgsConstructor;

/**
 * 창고 관리자 배정 포트를 창고 도메인의 멤버 배정 유스케이스에 연결한다. 창고가 정한 역할 코드는 이 어댑터만 안다.
 */
@Component
@RequiredArgsConstructor
public class WarehouseManagerAssignAdapter implements WarehouseManagerAssignPort {

    private static final String MANAGER_ROLE_CODE = "MANAGER";

    private final WarehouseMemberUseCase warehouseMemberUseCase;

    @Override
    public void assignManager(Long warehouseId, Long userId) {
        warehouseMemberUseCase.assignManager(
                new WarehouseMemberAssignCommand(warehouseId, userId, MANAGER_ROLE_CODE));
    }
}
