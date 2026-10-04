package com.kb.wms.storeorder.application.port.in;

/**
 * 지점 발주 존재 여부 조회 인바운드 포트(다른 도메인용 읽기 전용).
 *
 * <p>지점 비활성화처럼 다른 도메인이 "진행 중인 발주가 있는지"만 확인할 때 쓴다. 발주 등록·상태 전이 유스케이스
 * ({@link StoreOrderUseCase})와 분리해 두어 지점 도메인과 발주 도메인 사이에 순환 의존이 생기지 않게 한다.
 */
public interface StoreOrderPresenceUseCase {

    /** 지점에 진행 중인 발주(REQUESTED·APPROVED·ASSIGNED·ON_HOLD)가 하나라도 있으면 true. */
    boolean hasInProgressOrders(Long storeId);
}
