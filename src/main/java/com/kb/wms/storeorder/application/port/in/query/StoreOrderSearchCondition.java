package com.kb.wms.storeorder.application.port.in.query;

import java.time.LocalDateTime;

import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

/**
 * GET /api/v1/orders 검색 조건. null인 조건은 무시한다.
 * 점주·창고 관리자의 소속 범위 제한은 인증 연동 시 서비스에서 storeId·warehouseId로 좁혀 넘긴다.
 *
 * @param keyword       주문 번호 또는 지점명 부분 일치
 * @param requestedFrom 발주 요청 시작 일시 (이 시각 이후)
 * @param requestedTo   발주 요청 종료 일시 (이 시각 이전)
 */
public record StoreOrderSearchCondition(
        StoreOrderStatus status,
        Long storeId,
        Long warehouseId,
        String keyword,
        LocalDateTime requestedFrom,
        LocalDateTime requestedTo
) {
}
