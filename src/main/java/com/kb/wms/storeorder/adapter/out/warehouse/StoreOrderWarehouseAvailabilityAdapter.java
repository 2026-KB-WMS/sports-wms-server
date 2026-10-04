package com.kb.wms.storeorder.adapter.out.warehouse;

import org.springframework.stereotype.Component;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.storeorder.application.port.out.WarehouseAvailabilityPort;
import com.kb.wms.warehouse.application.port.in.WarehouseUseCase;

import lombok.RequiredArgsConstructor;

/**
 * 지점 발주 도메인의 창고 상태 포트를 창고 도메인 유스케이스에 연결한다.
 */
@Component
@RequiredArgsConstructor
public class StoreOrderWarehouseAvailabilityAdapter implements WarehouseAvailabilityPort {

    private final WarehouseUseCase warehouseUseCase;

    @Override
    public void requireExists(Long warehouseId) {
        warehouseUseCase.getWarehouse(warehouseId);
    }

    @Override
    public void requireActive(Long warehouseId) {
        if (!warehouseUseCase.getWarehouse(warehouseId).isActive()) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "비활성 창고는 배정하거나 재개할 수 없습니다. warehouseId=" + warehouseId);
        }
    }
}
