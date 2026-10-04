package com.kb.wms.storeorder.application.port.in.result;

import java.util.List;

import com.kb.wms.storeorder.domain.enums.StoreOrderProgressStage;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

/**
 * 지점 발주 상세 (GET /api/v1/orders/{orderId}/details). 항목·출고·상태 이력을 한 번에 담는다.
 * 출고 도메인 연동 전에는 outbounds가 빈 목록이다.
 */
public record StoreOrderDetails(
        Long storeOrderId,
        String orderNo,
        StoreOrderStatus status,
        StoreOrderProgressStage progressStage,
        List<StoreOrderLineView> items,
        List<StoreOrderOutboundView> outbounds,
        List<StoreOrderStatusHistoryView> statusHistory
) {
}
