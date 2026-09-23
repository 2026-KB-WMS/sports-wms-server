package com.kb.wms.inventory.domain.enums;

/**
 * 로트 상태.
 */
public enum LotStatus {
    /** 가용 */
    AVAILABLE,
    /** 만료 */
    EXPIRED,
    /** 격리 */
    QUARANTINED,
    /** 폐기 */
    DISPOSED
}
