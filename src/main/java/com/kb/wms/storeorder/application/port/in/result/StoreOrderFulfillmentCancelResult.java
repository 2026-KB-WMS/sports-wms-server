package com.kb.wms.storeorder.application.port.in.result;

/**
 * 승인 이후 발주 취소 때 출고 쪽에서 함께 정리한 건수 (PATCH /api/v1/orders/{orderId}/cancel 응답).
 *
 * @param releasedAllocationCount 해제(RELEASED)한 재고 할당 수
 * @param canceledOutboundCount   취소(CANCELED)한 READY 출고 수
 */
public record StoreOrderFulfillmentCancelResult(
        int releasedAllocationCount,
        int canceledOutboundCount
) {

    /** 정리할 할당·출고가 없었다. 출고 도메인 연동 전 임시 구현이 항상 돌려준다. */
    public static final StoreOrderFulfillmentCancelResult NONE = new StoreOrderFulfillmentCancelResult(0, 0);
}
