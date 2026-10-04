package com.kb.wms.storeorder.domain.enums;

/**
 * 지점 발주 항목 진행 상태 (StoreOrderLine).
 * REQUESTED(출고 전) → PARTIALLY_SHIPPED(일부 출고) → COMPLETED(전량 출고).
 * 발주가 취소·반려되면 REQUESTED 항목이 CANCELED가 된다.
 * 할당·피킹 단계는 항목 상태가 아니라 allocated_quantity와 StockAllocation·Outbound 상태로 확인한다.
 */
public enum StoreOrderLineStatus {
    REQUESTED,
    PARTIALLY_SHIPPED,
    COMPLETED,
    CANCELED
}
