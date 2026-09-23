package com.kb.wms.inventory.application.port.in;

import com.kb.wms.inventory.application.port.in.command.InventoryAdjustCommand;
import com.kb.wms.inventory.application.port.in.result.InventoryAdjustmentResult;

/**
 * 재고 조정 유스케이스. POST /api/v1/inventory/adjustments
 */
public interface InventoryAdjustmentUseCase {

    InventoryAdjustmentResult adjust(InventoryAdjustCommand command);
}
