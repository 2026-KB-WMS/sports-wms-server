package com.kb.wms.outbound.application.port.in.command;

/**
 * 발주 재고 할당 요청(POST /api/v1/allocations). 대상 창고는 발주에 배정된 창고다.
 *
 * @param storeOrderId 할당할 발주
 * @param userId       처리 사용자(토큰 사용자의 ID)
 */
public record StockAllocateCommand(
        Long storeOrderId,
        Long userId
) {
}