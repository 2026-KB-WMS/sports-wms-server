package com.kb.wms.inbound.adapter.out.warehouse;

import org.springframework.stereotype.Component;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.inbound.application.port.out.WarehouseAvailabilityPort;
import com.kb.wms.warehouse.application.port.in.WarehouseUseCase;

import lombok.RequiredArgsConstructor;

/**
 * 입고 도메인의 창고 상태 포트를 창고 도메인 유스케이스에 연결한다.
 */
@Component
@RequiredArgsConstructor
public class WarehouseAvailabilityAdapter implements WarehouseAvailabilityPort {

    private final WarehouseUseCase warehouseUseCase;

    @Override
    public void requireActive(Long warehouseId) {
        if (!warehouseUseCase.getWarehouse(warehouseId).isActive()) {
            throw new BusinessException(ErrorCode.CONFLICT, "비활성 창고에는 발주를 등록할 수 없습니다.");
        }
    }
}
