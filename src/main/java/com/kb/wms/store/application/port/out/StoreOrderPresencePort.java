package com.kb.wms.store.application.port.out;

/**
 * 지점 발주 존재 여부 아웃바운드 포트(지점 발주 도메인). 지점 비활성화 전에 진행 중인 발주를 확인한다.
 */
public interface StoreOrderPresencePort {

    /** 지점에 진행 중인 발주(REQUESTED·APPROVED·ASSIGNED·ON_HOLD)가 있으면 true. */
    boolean hasInProgressOrders(Long storeId);
}
