package com.kb.wms.outbound.domain.enums;

/**
 * 재고 할당 상태.
 *
 * <pre>
 * ALLOCATED → PICKED   (피킹 완료)
 * ALLOCATED → RELEASED (할당 해제)
 * </pre>
 */
public enum AllocationStatus {
    /** 재고가 예약된 상태 */
    ALLOCATED,
    /** 피킹 완료로 재고가 차감된 상태 */
    PICKED,
    /** 할당이 해제되어 예약 재고가 반환된 상태 */
    RELEASED
}
