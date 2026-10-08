package com.kb.wms.storeorder.application.port.in.command;

import java.time.LocalDateTime;
import java.util.List;

/**
 * POST /api/v1/orders 요청. 공급 단가는 요청에서 받지 않고 등록 시점의 SKU 공급 단가를 스냅샷한다.
 *
 * @param storeId             발주하는 지점
 * @param requestedDeliveryAt 요청 배송 일시 (선택, 현재 시각 이후)
 * @param note                발주 비고 (선택)
 * @param createdBy           요청 사용자 (토큰 사용자의 ID)
 */
public record StoreOrderRegisterCommand(
        Long storeId,
        LocalDateTime requestedDeliveryAt,
        String note,
        Long createdBy,
        List<Line> lines
) {

    /**
     * @param requestedQuantity 요청 수량 (1 이상)
     */
    public record Line(Long skuId, long requestedQuantity) {
    }
}
