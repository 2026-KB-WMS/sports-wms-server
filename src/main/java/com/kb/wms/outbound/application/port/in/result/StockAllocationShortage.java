package com.kb.wms.outbound.application.port.in.result;

/**
 * 재고가 모자라 요청 수량만큼 할당하지 못한 SKU. 이번 호출에서 할당하려던 잔여 수량 기준이다.
 *
 * @param requestedQuantity 이번 호출에서 할당하려던 수량(발주 항목의 잔여 수량)
 * @param allocatedQuantity 이번 호출에서 실제로 할당한 수량
 * @param shortageQuantity  할당하지 못한 수량
 */
public record StockAllocationShortage(
        Long skuId,
        long requestedQuantity,
        long allocatedQuantity,
        long shortageQuantity
) {
}
