package com.kb.wms.outbound.application.port.in.result;

/**
 * 발주 취소에 따라 출고 쪽에서 정리한 건수.
 *
 * @param releasedAllocationCount 해제(RELEASED)한 재고 할당 수
 * @param canceledOutboundCount   취소(CANCELED)한 READY 출고 수
 */
public record OrderFulfillmentCancelResult(
        int releasedAllocationCount,
        int canceledOutboundCount
) {
}
