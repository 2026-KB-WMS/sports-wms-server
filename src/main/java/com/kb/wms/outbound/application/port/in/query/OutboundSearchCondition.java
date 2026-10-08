package com.kb.wms.outbound.application.port.in.query;

import java.time.LocalDateTime;
import java.util.List;

import com.kb.wms.outbound.domain.enums.OutboundStatus;

/**
 * GET /api/v1/outbounds 검색 조건. null은 조건 없음이다.
 *
 * @param warehouseId 발주에 배정된 창고 기준
 * @param keyword     출고 번호·발주 번호 부분 일치
 * @param warehouseIds 조회를 허용할 창고 범위(담당 창고). null이면 제한 없음
 */
public record OutboundSearchCondition(
        OutboundStatus status,
        Long warehouseId,
        Long storeId,
        Long storeOrderId,
        String keyword,
        LocalDateTime createdFrom,
        LocalDateTime createdTo,
        List<Long> warehouseIds
) {

    public OutboundSearchCondition(OutboundStatus status, Long warehouseId, Long storeId, Long storeOrderId,
                                   String keyword, LocalDateTime createdFrom, LocalDateTime createdTo) {
        this(status, warehouseId, storeId, storeOrderId, keyword, createdFrom, createdTo, null);
    }
}