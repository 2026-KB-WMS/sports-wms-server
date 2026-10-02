package com.kb.wms.inbound.domain.enums;

/**
 * 창고 발주 진행 상태 (PurchaseOrder).
 * REQUESTED(창고 관리자 등록) → CONFIRMED(본사 관리자 확정) → COMPLETED(전량 입고 시 시스템 자동 전환).
 * REQUESTED·CONFIRMED에서는 CANCELED로 전환할 수 있다.
 */
public enum PurchaseOrderStatus {
    REQUESTED,
    CONFIRMED,
    COMPLETED,
    CANCELED
}
