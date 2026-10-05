package com.kb.wms.outbound.application.port.in.result;

/**
 * 재고 할당 상세. outboundId는 취소되지 않은 출고에 연결된 경우에만 값이 있다.
 */
public record StockAllocationDetail(
        StockAllocationView view,
        Long outboundId
) {
}