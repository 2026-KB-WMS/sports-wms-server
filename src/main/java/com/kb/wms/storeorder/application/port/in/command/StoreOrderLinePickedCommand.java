package com.kb.wms.storeorder.application.port.in.command;

/**
 * 발주 항목 하나의 피킹 완료 반영 요청. 할당 수량을 줄이고 출고 수량을 늘린다.
 *
 * @param storeOrderLineId  발주 항목 ID
 * @param allocatedQuantity 이번에 피킹 처리하는 할당 수량(줄일 할당 수량)
 * @param pickedQuantity    실제 피킹 수량(늘릴 출고 수량, 0 이상 할당 수량 이하)
 */
public record StoreOrderLinePickedCommand(
        Long storeOrderLineId,
        long allocatedQuantity,
        long pickedQuantity
) {
}
