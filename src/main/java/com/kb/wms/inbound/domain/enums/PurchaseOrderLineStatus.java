package com.kb.wms.inbound.domain.enums;

/**
 * 창고 발주 항목 진행 상태 (PurchaseOrderLine).
 * REQUESTED(입고 전) → PARTIALLY_RECEIVED(일부 입고, received &lt; expected) → COMPLETED(전량 입고).
 */
public enum PurchaseOrderLineStatus {
    REQUESTED,
    PARTIALLY_RECEIVED,
    COMPLETED
}
