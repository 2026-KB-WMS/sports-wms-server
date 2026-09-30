package com.kb.wms.inbound.domain.enums;

/**
 * 공급처 거래 상태 (Supplier).
 * 공급처는 삭제하지 않고 비활성화(ACTIVE → INACTIVE)만 허용한다.
 */
public enum SupplierStatus {
    ACTIVE,
    INACTIVE;

    /** isActive 필터 값을 상태로 바꾼다. null이면 조건 없음(null). */
    public static SupplierStatus fromActiveFlag(Boolean isActive) {
        if (isActive == null) {
            return null;
        }
        return isActive ? ACTIVE : INACTIVE;
    }
}
