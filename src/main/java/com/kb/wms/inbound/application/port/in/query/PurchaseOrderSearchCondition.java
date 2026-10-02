package com.kb.wms.inbound.application.port.in.query;

import java.time.LocalDateTime;

import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;

/**
 * GET /api/v1/purchase-orders 검색 조건. null인 조건은 무시한다.
 * 창고 관리자의 담당 창고 범위 제한은 인증 연동 시 웹 어댑터/서비스에서 warehouseId로 좁혀 넘긴다.
 *
 * @param keyword     발주 번호 부분 일치
 * @param createdFrom 등록 시작 일시 (이 시각 이후)
 * @param createdTo   등록 종료 일시 (이 시각 이전)
 */
public record PurchaseOrderSearchCondition(
        PurchaseOrderStatus status,
        Long warehouseId,
        Long supplierId,
        String keyword,
        LocalDateTime createdFrom,
        LocalDateTime createdTo
) {
}
