package com.kb.wms.inventory.domain.enums;

/**
 * 재고 트랜잭션이 참조하는 원천 문서 유형.
 */
public enum ReferenceType {
    /** 입고(Inbound) */
    INBOUND,
    /** 출고(Outbound) */
    OUTBOUND,
    /** 재고 조정 (원천 문서 없음, reference_id NULL) */
    ADJUSTMENT
}
