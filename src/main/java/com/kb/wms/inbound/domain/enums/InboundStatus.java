package com.kb.wms.inbound.domain.enums;

/**
 * 입고 진행 상태 (Inbound).
 * ARRIVED(도착) → INSPECTING(검수 중) → COMPLETED(입고 완료, 재고 반영).
 * ARRIVED·INSPECTING에서만 CANCELED로 전환할 수 있으며, COMPLETED 이후에는 취소할 수 없다.
 */
public enum InboundStatus {
    ARRIVED,
    INSPECTING,
    COMPLETED,
    CANCELED
}
