package com.kb.wms.warehouse.adapter.out.inventory;

import org.springframework.stereotype.Component;

import com.kb.wms.inventory.application.port.in.InventoryQueryUseCase;
import com.kb.wms.warehouse.application.port.out.StockPresencePort;

import lombok.RequiredArgsConstructor;

/**
 * 창고 도메인의 재고 존재 확인 포트를 재고 도메인 조회 유스케이스로 연결한다.
 */
@Component
@RequiredArgsConstructor
public class StockPresenceAdapter implements StockPresencePort {

    private final InventoryQueryUseCase inventoryQueryUseCase;

    @Override
    public boolean hasStockInSection(Long sectionId) {
        return inventoryQueryUseCase.hasStockInSection(sectionId);
    }

    @Override
    public boolean hasStockInWarehouse(Long warehouseId) {
        return inventoryQueryUseCase.hasStockInWarehouse(warehouseId);
    }
}
