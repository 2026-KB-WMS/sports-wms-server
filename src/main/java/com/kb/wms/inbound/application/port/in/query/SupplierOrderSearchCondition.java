package com.kb.wms.inbound.application.port.in.query;

import java.time.LocalDateTime;
import java.util.List;

import com.kb.wms.inbound.domain.enums.SupplierOrderStatus;

/**
 * GET /api/v1/supplier-orders 검색 조건. null인 조건은 무시한다.
 *
 * @param keyword      발주서 번호 또는 발주 요청 번호 부분 일치
 * @param createdFrom  등록 시작 일시 (이 시각 이후)
 * @param createdTo    등록 종료 일시 (이 시각 이전)
 * @param warehouseIds 조회를 허용할 창고 범위(담당 창고). null이면 제한 없음
 */
public record SupplierOrderSearchCondition(
        SupplierOrderStatus status,
        Long warehouseId,
        Long supplierId,
        Long purchaseOrderId,
        String keyword,
        LocalDateTime createdFrom,
        LocalDateTime createdTo,
        List<Long> warehouseIds
) {

    /** 담당 창고 범위 제한 없이 조회하는 조건. 사용자 요청에서는 서비스가 범위를 채워 넘긴다. */
    public static SupplierOrderSearchCondition unscoped(SupplierOrderStatus status, Long warehouseId, Long supplierId,
            Long purchaseOrderId, String keyword, LocalDateTime createdFrom, LocalDateTime createdTo) {
        return new SupplierOrderSearchCondition(status, warehouseId, supplierId, purchaseOrderId, keyword,
                createdFrom, createdTo, null);
    }
}
