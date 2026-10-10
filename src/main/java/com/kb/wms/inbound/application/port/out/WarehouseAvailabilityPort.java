package com.kb.wms.inbound.application.port.out;

/**
 * 창고 도메인의 창고 상태를 입고 도메인에서 확인하기 위한 아웃바운드 포트.
 */
public interface WarehouseAvailabilityPort {

    /** 창고가 없으면 창고 도메인의 404 WAREHOUSE_NOT_FOUND 예외를 던진다. 목록 조회의 warehouseId 필터 검증에 쓴다. */
    void requireExists(Long warehouseId);

    /** 창고가 없으면 창고 도메인의 WAREHOUSE_NOT_FOUND, 비활성이면 409 CONFLICT 예외를 던진다. */
    void requireActive(Long warehouseId);
}
