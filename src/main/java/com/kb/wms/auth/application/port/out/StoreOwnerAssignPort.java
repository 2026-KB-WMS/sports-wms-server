package com.kb.wms.auth.application.port.out;

/**
 * 승인한 점주를 지점에 배정하는 아웃바운드 포트. 구현은 지점 도메인의 `port.in`에 연결하는 어댑터가 맡는다.
 */
public interface StoreOwnerAssignPort {

    /** 사용자를 지점 대표(OWNER)로 배정한다. 비활성 지점 등은 지점 도메인 오류로 던져진다. */
    void assignOwner(Long storeId, Long userId);
}
