package com.kb.wms.storeorder.application.port.in.query;

import java.time.LocalDateTime;
import java.util.List;

import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

/**
 * GET /api/v1/orders, GET /api/v1/orders/my 검색 조건. null인 조건은 무시한다.
 *
 * @param keyword       주문 번호 또는 지점명 부분 일치
 * @param requestedFrom 발주 요청 시작 일시 (이 시각 이후)
 * @param requestedTo   발주 요청 종료 일시 (이 시각 이전)
 * @param storeIds      조회를 허용할 지점 범위(점주의 담당 지점). null이면 제한 없음
 * @param warehouseIds  조회를 허용할 창고 범위(창고 관리자의 담당 창고). null이면 제한 없음.
 *                      범위를 주면 창고가 배정되지 않은 발주는 제외된다.
 */
public record StoreOrderSearchCondition(
        StoreOrderStatus status,
        Long storeId,
        Long warehouseId,
        String keyword,
        LocalDateTime requestedFrom,
        LocalDateTime requestedTo,
        List<Long> storeIds,
        List<Long> warehouseIds
) {

    public StoreOrderSearchCondition(StoreOrderStatus status, Long storeId, Long warehouseId, String keyword,
                                     LocalDateTime requestedFrom, LocalDateTime requestedTo) {
        this(status, storeId, warehouseId, keyword, requestedFrom, requestedTo, null, null);
    }
}
