package com.kb.wms.inbound.application.port.in.query;

import java.time.LocalDateTime;

import com.kb.wms.inbound.domain.enums.InboundStatus;

/**
 * GET /api/v1/inbounds 검색 조건. 모든 필드는 선택이며 null이면 해당 조건을 적용하지 않는다.
 *
 * @param keyword     입고 번호·발주 번호 부분 일치 (대소문자 무시)
 * @param arrivedFrom 도착 시작 일시 (이 시각 이후)
 * @param arrivedTo   도착 종료 일시 (이 시각 이전)
 */
public record InboundSearchCondition(
        InboundStatus status,
        Long warehouseId,
        Long purchaseOrderId,
        String keyword,
        LocalDateTime arrivedFrom,
        LocalDateTime arrivedTo
) {
}
