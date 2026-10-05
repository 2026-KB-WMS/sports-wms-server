package com.kb.wms.storeorder.application.port.in.result;

import java.time.LocalDateTime;
import java.util.List;

import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

/**
 * 부분 출고 종결 처리 결과 (PATCH /api/v1/orders/{orderId}/complete-partial 응답).
 *
 * @param items 항목별 요청·출고·부족 수량 (SKU 코드 순)
 * @param releasedAllocationCount 종결하며 해제한 남은 ALLOCATED 재고 할당 건수
 */
public record StoreOrderCompletePartialResult(
        Long storeOrderId,
        String orderNo,
        StoreOrderStatus status,
        String statusReason,
        List<Item> items,
        int releasedAllocationCount,
        LocalDateTime updatedAt
) {

    /**
     * @param shortageQuantity 요청 수량에서 출고 수량을 뺀 부족분. 초과 출고여도 음수가 되지 않는다.
     */
    public record Item(
            Long storeOrderLineId,
            String skuCode,
            Long requestedQuantity,
            Long shippedQuantity,
            Long shortageQuantity
    ) {
    }
}
