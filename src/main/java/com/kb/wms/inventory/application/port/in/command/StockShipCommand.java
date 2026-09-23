package com.kb.wms.inventory.application.port.in.command;

/**
 * 피킹 완료 시 한 재고 행의 출고 반영.
 * 보유 수량은 pickedQuantity만큼, 할당 수량은 allocatedQuantity(할당 전체)만큼 줄인다.
 * 부족분(allocated - picked)의 예약은 풀려 다시 가용 재고가 된다.
 *
 * @param outboundId InventoryTransaction.reference_id
 * @param userId     처리 사용자
 */
public record StockShipCommand(
        Long inventoryLotId,
        long allocatedQuantity,
        long pickedQuantity,
        Long outboundId,
        Long userId
) {
}
