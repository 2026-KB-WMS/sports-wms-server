package com.kb.wms.storeorder.adapter.out.warehouse;

import org.springframework.stereotype.Component;

import com.kb.wms.storeorder.application.port.out.WarehouseExistencePort;
import com.kb.wms.warehouse.application.port.in.WarehouseUseCase;

import lombok.RequiredArgsConstructor;

/**
 * 지점 발주 도메인의 창고 존재 포트를 창고 도메인 유스케이스에 연결한다.
 */
@Component
@RequiredArgsConstructor
public class WarehouseExistenceAdapter implements WarehouseExistencePort {

    private final WarehouseUseCase warehouseUseCase;

    @Override
    public void requireExists(Long warehouseId) {
        warehouseUseCase.getWarehouse(warehouseId);
    }
}
