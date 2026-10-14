package com.kb.wms.storeorder.application.port.in.result;

import java.math.BigDecimal;

/**
 * 출고 연동용 지점 발주 항목. 출고 도메인이 발주 엔티티를 직접 쓰지 않도록 필요한 값만 담아 돌려준다.
 *
 * @param unallocatedQuantity     아직 재고를 할당하지 못한 수량(요청 수량 - 출고 수량 - 할당 수량)
 * @param requestedUnitSupplyPrice 발주 시점 공급 단가 스냅샷(없을 수 있다)
 */
public record StoreOrderFulfillmentLineView(
        Long storeOrderLineId,
        Long skuId,
        long unallocatedQuantity,
        BigDecimal requestedUnitSupplyPrice
) {
}
