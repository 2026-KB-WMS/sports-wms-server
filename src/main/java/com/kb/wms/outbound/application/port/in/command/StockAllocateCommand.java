package com.kb.wms.outbound.application.port.in.command;

import java.util.List;

/**
 * 발주 재고 할당 요청(POST /api/v1/allocations). 대상 창고는 발주에 배정된 창고다.
 *
 * @param storeOrderId 할당할 발주
 * @param userId       처리 사용자(토큰 사용자의 ID)
 * @param skuIds       할당할 SKU. null이면 발주 전체, 비어 있으면 거절한다.
 */
public record StockAllocateCommand(
        Long storeOrderId,
        Long userId,
        List<Long> skuIds
) {

    /** 발주 전체를 할당한다. */
    public StockAllocateCommand(Long storeOrderId, Long userId) {
        this(storeOrderId, userId, null);
    }
}
