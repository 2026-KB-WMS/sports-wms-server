package com.kb.wms.auth.application.port.out;

/**
 * 승인한 창고 관리자를 창고에 배정하는 아웃바운드 포트. 구현은 창고 도메인의 `port.in`에 연결하는 어댑터가 맡는다.
 */
public interface WarehouseManagerAssignPort {

    /** 사용자를 창고 담당(MANAGER)으로 배정한다. 비활성 창고 등은 창고 도메인 오류로 던져진다. */
    void assignManager(Long warehouseId, Long userId);
}
