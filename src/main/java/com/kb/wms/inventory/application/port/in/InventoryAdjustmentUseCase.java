package com.kb.wms.inventory.application.port.in;

import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.inventory.application.port.in.command.InventoryAdjustCommand;
import com.kb.wms.inventory.application.port.in.result.InventoryAdjustmentResult;

/**
 * 재고 조정 유스케이스. POST /api/v1/inventory/adjustments
 */
public interface InventoryAdjustmentUseCase {

    /** 재고 행이 속한 창고가 actor의 담당 창고가 아니면 403 FORBIDDEN. */
    InventoryAdjustmentResult adjust(InventoryAdjustCommand command, AuthenticatedUser actor);
}
