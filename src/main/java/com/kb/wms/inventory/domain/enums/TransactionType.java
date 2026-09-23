package com.kb.wms.inventory.domain.enums;

/**
 * 재고 트랜잭션(수량 변동) 유형.
 */
public enum TransactionType {
    /** 입고 */
    INBOUND,
    /** 출고 */
    OUTBOUND,
    /** 조정 */
    ADJUSTMENT
}
