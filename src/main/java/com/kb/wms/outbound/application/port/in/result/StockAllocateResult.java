package com.kb.wms.outbound.application.port.in.result;

import java.util.List;

/**
 * 재고 할당 결과. 항목은 만들어진 할당(재고 행 단위)이며 한 발주 항목이 여러 재고 행에 나뉠 수 있다.
 */
public record StockAllocateResult(
        Long storeOrderId,
        String orderNo,
        List<StockAllocationSummary> items
) {
}