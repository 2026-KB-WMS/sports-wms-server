package com.kb.wms.storeorder.application.port.out;

/**
 * 창고 도메인의 창고 존재·활성 여부를 지점 발주 도메인에서 확인하기 위한 아웃바운드 포트.
 * 목록 조회의 warehouseId 필터 검증과 창고 배정·재개 검증에 쓴다.
 */
public interface WarehouseAvailabilityPort {

    /** 창고가 없으면 창고 도메인의 404 WAREHOUSE_NOT_FOUND 예외를 던진다. */
    void requireExists(Long warehouseId);

    /** 창고가 없으면 404 WAREHOUSE_NOT_FOUND, 비활성이면 409 CONFLICT 예외를 던진다. */
    void requireActive(Long warehouseId);
}
