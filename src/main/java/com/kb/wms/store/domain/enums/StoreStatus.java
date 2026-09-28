package com.kb.wms.store.domain.enums;

/**
 * 지점 운영 상태 (Store).
 * 지점은 삭제하지 않고 비활성화(ACTIVE → INACTIVE)만 허용한다.
 */
public enum StoreStatus {
    ACTIVE,
    INACTIVE;

    /** isActive 필터 값을 상태로 바꾼다. null이면 조건 없음(null). */
    public static StoreStatus fromActiveFlag(Boolean isActive) {
        if (isActive == null) {
            return null;
        }
        return isActive ? ACTIVE : INACTIVE;
    }
}
