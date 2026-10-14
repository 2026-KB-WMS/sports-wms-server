package com.kb.wms.outbound.domain.enums;

/**
 * 출고가 보는 지점 발주 상태. 지점 발주 도메인의 enum을 직접 쓰지 않고, 어댑터가 이름으로 변환해 채운다.
 * 이 enum에 없는 값은 변환에서 거절되므로 발주 쪽에 상태가 추가되면 여기와 어댑터를 함께 갱신한다.
 */
public enum StoreOrderState {
    REQUESTED,
    APPROVED,
    ASSIGNED,
    ON_HOLD,
    COMPLETED,
    CANCELED,
    REJECTED
}
