package com.kb.wms.inventory.application.port.in.command;

/**
 * POST /api/v1/inventory/adjustments.
 *
 * @param beforeQuantity 화면에서 확인한 현재 보유 수량. 서버 값과 다르면 조정하지 않는다(STALE_QUANTITY).
 * @param afterQuantity  조정 후 보유 수량 (0 이상, 할당 수량 이상)
 * @param userId         처리 사용자 (InventoryTransaction.created_by)
 */
public record InventoryAdjustCommand(
        Long inventoryLotId,
        Long beforeQuantity,
        Long afterQuantity,
        String reason,
        Long userId
) {
}
