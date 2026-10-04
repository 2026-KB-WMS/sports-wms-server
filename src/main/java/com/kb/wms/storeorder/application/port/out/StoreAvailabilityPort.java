package com.kb.wms.storeorder.application.port.out;

/**
 * 지점 도메인의 지점 존재·활성 여부를 지점 발주 도메인에서 확인하기 위한 아웃바운드 포트.
 */
public interface StoreAvailabilityPort {

    /** 지점이 없으면 지점 도메인의 404 STORE_NOT_FOUND 예외를 던진다. */
    void requireExists(Long storeId);

    /** 지점이 없으면 404 STORE_NOT_FOUND, 비활성이면 409 CONFLICT 예외를 던진다. */
    void requireActive(Long storeId);
}
