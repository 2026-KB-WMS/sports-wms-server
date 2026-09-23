package com.kb.wms.inventory.application.port.in.result;

import com.kb.wms.inventory.domain.entity.InventoryLot;
import com.kb.wms.inventory.domain.entity.InventoryTransaction;

/**
 * 재고 조정 결과. 남긴 이력과 조정 후 재고 행.
 */
public record InventoryAdjustmentResult(
        InventoryTransaction transaction,
        InventoryLot inventoryLot
) {
}
