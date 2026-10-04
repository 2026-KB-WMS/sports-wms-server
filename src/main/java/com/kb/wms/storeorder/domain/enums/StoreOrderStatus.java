package com.kb.wms.storeorder.domain.enums;

/**
 * 지점 발주 진행 상태 (StoreOrder).
 * REQUESTED → APPROVED → ASSIGNED ↔ ON_HOLD, ASSIGNED → COMPLETED(전량 또는 부분 출고 종결).
 * REQUESTED는 REJECTED(반려)가 될 수 있고, 종결 전 상태(REQUESTED·APPROVED·ASSIGNED·ON_HOLD)는 CANCELED가 될 수 있다.
 * CANCELED·REJECTED·COMPLETED는 종결 상태다.
 */
public enum StoreOrderStatus {
    REQUESTED,
    APPROVED,
    ASSIGNED,
    ON_HOLD,
    COMPLETED,
    CANCELED,
    REJECTED
}
