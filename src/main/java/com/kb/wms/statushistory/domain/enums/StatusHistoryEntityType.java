package com.kb.wms.statushistory.domain.enums;

/**
 * 상태 이력이 가리키는 업무 엔티티 유형. status_history.entity_id가 어느 테이블의 PK인지 결정한다.
 */
public enum StatusHistoryEntityType {
    USER,
    PURCHASE_ORDER,
    INBOUND,
    STORE_ORDER,
    STOCK_ALLOCATION,
    OUTBOUND,
    STORE,
    LOT
}
