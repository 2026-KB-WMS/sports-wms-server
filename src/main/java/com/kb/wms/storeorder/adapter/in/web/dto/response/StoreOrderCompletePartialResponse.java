package com.kb.wms.storeorder.adapter.in.web.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.kb.wms.storeorder.application.port.in.result.StoreOrderCompletePartialResult;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

/**
 * PATCH /api/v1/orders/{orderId}/complete-partial 응답. 항목별 부족 수량을 함께 담는다.
 */
public record StoreOrderCompletePartialResponse(
        Long storeOrderId,
        String orderNo,
        StoreOrderStatus status,
        String statusReason,
        List<Item> items,
        int releasedAllocationCount,
        LocalDateTime updatedAt
) {

    public record Item(
            Long storeOrderLineId,
            String skuCode,
            Long requestedQuantity,
            Long shippedQuantity,
            Long shortageQuantity
    ) {
    }

    public static StoreOrderCompletePartialResponse from(StoreOrderCompletePartialResult result) {
        return new StoreOrderCompletePartialResponse(
                result.storeOrderId(), result.orderNo(), result.status(), result.statusReason(),
                result.items().stream()
                        .map(i -> new Item(i.storeOrderLineId(), i.skuCode(), i.requestedQuantity(),
                                i.shippedQuantity(), i.shortageQuantity()))
                        .toList(),
                result.releasedAllocationCount(), result.updatedAt());
    }
}
