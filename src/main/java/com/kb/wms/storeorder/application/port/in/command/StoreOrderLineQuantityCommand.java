package com.kb.wms.storeorder.application.port.in.command;

/**
 * 발주 항목 하나의 할당 수량 변경 요청. 재고 할당(증가)·할당 해제(감소)에서 쓴다.
 *
 * @param storeOrderLineId 발주 항목 ID
 * @param quantity         변경할 수량(1 이상)
 */
public record StoreOrderLineQuantityCommand(
        Long storeOrderLineId,
        long quantity
) {
}
