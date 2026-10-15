package com.kb.wms.inbound.domain.enums;

/**
 * 창고 발주·입고 도메인이 상태 이력을 남기는 엔티티 종류. 이력 모듈에는 {@code name()} 문자열로 넘기며,
 * 값은 status_history.entity_type에 그대로 저장된다. 기존 값을 바꾸면 저장된 이력과 어긋난다.
 */
public enum InboundHistoryEntityType {
    PURCHASE_ORDER,
    SUPPLIER_ORDER,
    INBOUND
}
