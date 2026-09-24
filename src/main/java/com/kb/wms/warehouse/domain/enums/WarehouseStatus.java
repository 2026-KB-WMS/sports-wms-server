package com.kb.wms.warehouse.domain.enums;

/**
 * 창고 도메인 공통 사용 여부 상태 (Warehouse, WarehouseSection).
 */
public enum WarehouseStatus {
    ACTIVE,
    INACTIVE;

    /** isActive 필터 값을 상태로 바꾼다. null이면 조건 없음(null). */
    public static WarehouseStatus fromActiveFlag(Boolean isActive) {
        if (isActive == null) {
            return null;
        }
        return isActive ? ACTIVE : INACTIVE;
    }
}
