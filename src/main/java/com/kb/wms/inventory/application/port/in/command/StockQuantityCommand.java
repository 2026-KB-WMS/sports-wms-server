package com.kb.wms.inventory.application.port.in.command;

/**
 * 재고 행 하나와 수량. 할당·할당 해제에 쓴다.
 */
public record StockQuantityCommand(
        Long inventoryLotId,
        long quantity
) {
}
