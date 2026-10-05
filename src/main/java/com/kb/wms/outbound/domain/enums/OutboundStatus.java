package com.kb.wms.outbound.domain.enums;

/**
 * 출고 상태.
 *
 * <pre>
 * READY → PICKING → PICKED → SHIPPED → DELIVERED
 * READY → CANCELED (READY 에서만 취소 가능)
 * </pre>
 */
public enum OutboundStatus {
    /** 출고 생성, 피킹 대기 */
    READY,
    /** 피킹 진행 중 */
    PICKING,
    /** 피킹 완료 (재고 차감 완료) */
    PICKED,
    /** 출고(상차) 완료 */
    SHIPPED,
    /** 배송 완료 */
    DELIVERED,
    /** 취소 */
    CANCELED
}
