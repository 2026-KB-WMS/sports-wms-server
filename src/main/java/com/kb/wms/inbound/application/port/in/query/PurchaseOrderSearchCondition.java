package com.kb.wms.inbound.application.port.in.query;

import java.time.LocalDateTime;
import java.util.List;

import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;

/**
 * GET /api/v1/purchase-orders 검색 조건. null인 조건은 무시한다.
 *
 * @param keyword     발주 번호 부분 일치
 * @param createdFrom 등록 시작 일시 (이 시각 이후)
 * @param createdTo   등록 종료 일시 (이 시각 이전)
 * @param warehouseIds 조회를 허용할 창고 범위(담당 창고). null이면 제한 없음
 */
public record PurchaseOrderSearchCondition(
        PurchaseOrderStatus status,
        Long warehouseId,
        Long supplierId,
        String keyword,
        LocalDateTime createdFrom,
        LocalDateTime createdTo,
        List<Long> warehouseIds
) {

    public PurchaseOrderSearchCondition(PurchaseOrderStatus status, Long warehouseId, Long supplierId, String keyword,
                                        LocalDateTime createdFrom, LocalDateTime createdTo) {
        this(status, warehouseId, supplierId, keyword, createdFrom, createdTo, null);
    }
}
