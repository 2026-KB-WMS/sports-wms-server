package com.kb.wms.inventory.application.port.in.query;

import java.time.LocalDate;
import java.util.List;

/**
 * GET /api/v1/lots 검색 조건.
 *
 * @param expiringBefore 이 날짜(당일 포함) 이전에 만료되는 로트만
 * @param keyword        로트 번호 부분 일치
 * @param warehouseIds   이 창고들에 재고 또는 입고 완료 이력이 있는 로트만(담당 창고). null이면 제한 없음
 */
public record LotSearchCondition(
        Long skuId,
        Long supplierId,
        LocalDate expiringBefore,
        String keyword,
        List<Long> warehouseIds
) {
    /** 담당 창고·지점 범위 제한 없이 조회하는 조건. 사용자 요청에서는 서비스가 범위를 채워 넘긴다. */
    public static LotSearchCondition unscoped(Long skuId, Long supplierId, LocalDate expiringBefore, String keyword) {
        return new LotSearchCondition(skuId, supplierId, expiringBefore, keyword, null);
    }
}
