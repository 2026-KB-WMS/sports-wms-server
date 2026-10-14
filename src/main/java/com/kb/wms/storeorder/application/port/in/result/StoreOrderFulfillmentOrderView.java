package com.kb.wms.storeorder.application.port.in.result;

import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

/**
 * 출고 연동용 지점 발주 헤더. 출고 도메인이 발주 엔티티를 직접 쓰지 않도록 필요한 값만 담아 돌려준다.
 */
public record StoreOrderFulfillmentOrderView(
        Long storeOrderId,
        String orderNo,
        Long warehouseId,
        StoreOrderStatus status
) {
}
