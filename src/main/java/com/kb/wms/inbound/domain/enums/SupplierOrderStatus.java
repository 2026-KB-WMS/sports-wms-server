package com.kb.wms.inbound.domain.enums;

/**
 * 공급처 발주서 진행 상태 (SupplierOrder).
 * ISSUED(확정 시 생성) → PARTIALLY_RECEIVED(일부 입고 완료) → COMPLETED(소속 라인 전량 입고).
 * ISSUED·PARTIALLY_RECEIVED에서는 요청 취소에 따라 CANCELED로 전환된다.
 */
public enum SupplierOrderStatus {
    ISSUED,
    PARTIALLY_RECEIVED,
    COMPLETED,
    CANCELED
}
