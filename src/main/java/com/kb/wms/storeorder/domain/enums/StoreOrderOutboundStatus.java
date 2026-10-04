package com.kb.wms.storeorder.domain.enums;

/**
 * 지점 발주에 딸린 출고의 상태를 지점 발주 쪽에서 읽는 값. 출고 도메인의 출고 진행 상태와 같은 이름을 쓴다.
 * 지점 발주 도메인이 출고 도메인 패키지에 의존하지 않도록 여기에 따로 둔다. 연동 어댑터가 출고 상태를 이 값으로 옮긴다.
 * READY(출고 준비) → PICKING(피킹 중) → PICKED(피킹 완료) → SHIPPED(배송 중) → DELIVERED(배송 완료), CANCELED(취소).
 */
public enum StoreOrderOutboundStatus {
    READY,
    PICKING,
    PICKED,
    SHIPPED,
    DELIVERED,
    CANCELED;

    /** 피킹이 시작된 이후 상태(PICKING·PICKED·SHIPPED·DELIVERED). 발주를 취소할 수 없게 만드는 기준이다. */
    public boolean isPickingStarted() {
        return this == PICKING || this == PICKED || this == SHIPPED || this == DELIVERED;
    }

    /** 아직 끝나지 않은 출고(READY·PICKING·PICKED·SHIPPED). 부분 출고 종결을 막는 기준이다. */
    public boolean isInProgress() {
        return this == READY || this == PICKING || this == PICKED || this == SHIPPED;
    }
}
