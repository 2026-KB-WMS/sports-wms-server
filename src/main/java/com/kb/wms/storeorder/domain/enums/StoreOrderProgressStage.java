package com.kb.wms.storeorder.domain.enums;

/**
 * 점주용 진행 단계. DB에 저장하지 않는 읽기 전용 파생값이며 {@link #resolve}로 계산한다.
 * 발주 상태 + 가장 최근 출고 상태 + 부족 수량 여부가 입력이다. 매핑 표는 docs/api/store-order.md "공통 정의"에 있다.
 */
public enum StoreOrderProgressStage {
    /** 승인 대기 */
    PENDING_APPROVAL,
    /** 반려됨 */
    REJECTED,
    /** 취소됨 */
    CANCELED,
    /** 승인됨 (창고 배정 중) */
    AWAITING_ASSIGNMENT,
    /** 상품 준비 중 (할당·출고 준비·피킹을 묶어서 표현) */
    PREPARING,
    /** 배송 중 */
    IN_TRANSIT,
    /** 일부 배송 완료 (남은 수량 준비 중) */
    PARTIALLY_DELIVERED,
    /** 재고 확보 중 */
    ON_HOLD,
    /** 배송 완료 (부족 수량 없음) */
    COMPLETED,
    /** 일부 수량만 배송 완료 (부족 수량 있음) */
    COMPLETED_PARTIAL;

    /**
     * @param status               발주 상태
     * @param latestOutboundStatus 가장 최근 출고의 상태. 출고가 없으면 null
     * @param hasShortage          요청 수량에 못 미치게 출고된 항목이 있는지. COMPLETED일 때만 의미가 있다
     */
    public static StoreOrderProgressStage resolve(StoreOrderStatus status,
                                                  StoreOrderOutboundStatus latestOutboundStatus,
                                                  boolean hasShortage) {
        return switch (status) {
            case REQUESTED -> PENDING_APPROVAL;
            case REJECTED -> REJECTED;
            case CANCELED -> CANCELED;
            case APPROVED -> AWAITING_ASSIGNMENT;
            case ON_HOLD -> ON_HOLD;
            case COMPLETED -> hasShortage ? COMPLETED_PARTIAL : COMPLETED;
            case ASSIGNED -> resolveAssigned(latestOutboundStatus);
        };
    }

    // 전량 배송 완료는 시스템이 발주를 COMPLETED로 바꾸므로, ASSIGNED인데 최근 출고가 DELIVERED면 일부만 배송된 상태다.
    private static StoreOrderProgressStage resolveAssigned(StoreOrderOutboundStatus latestOutboundStatus) {
        if (latestOutboundStatus == StoreOrderOutboundStatus.SHIPPED) {
            return IN_TRANSIT;
        }
        if (latestOutboundStatus == StoreOrderOutboundStatus.DELIVERED) {
            return PARTIALLY_DELIVERED;
        }
        return PREPARING;
    }
}
