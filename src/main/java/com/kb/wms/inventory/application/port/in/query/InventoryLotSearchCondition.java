package com.kb.wms.inventory.application.port.in.query;

import java.time.LocalDate;
import java.util.List;

import com.kb.wms.inventory.domain.enums.QualityStatus;

/**
 * GET /api/v1/inventory/by-lot (로트·구역 단위) 검색 조건. lotId는 로트 상세의 구역별 재고 분포 조회에 쓴다.
 *
 * @param expiringBefore 이 날짜(당일 포함) 이전에 만료되는 로트만
 * @param includeEmpty   보유 수량 0인 행 포함 여부 (null이면 false)
 * @param warehouseIds   조회를 허용할 창고 범위(담당 창고). null이면 제한 없음
 */
public record InventoryLotSearchCondition(
        Long skuId,
        Long warehouseId,
        Long sectionId,
        Long lotId,
        LocalDate expiringBefore,
        QualityStatus qualityStatus,
        Boolean includeEmpty,
        List<Long> warehouseIds
) {

    public InventoryLotSearchCondition(Long skuId, Long warehouseId, Long sectionId, Long lotId,
                                       LocalDate expiringBefore, QualityStatus qualityStatus, Boolean includeEmpty) {
        this(skuId, warehouseId, sectionId, lotId, expiringBefore, qualityStatus, includeEmpty, null);
    }

    public static InventoryLotSearchCondition ofLot(Long lotId) {
        return ofLot(lotId, null);
    }

    public static InventoryLotSearchCondition ofLot(Long lotId, List<Long> warehouseIds) {
        return new InventoryLotSearchCondition(null, null, null, lotId, null, null, true, warehouseIds);
    }
}
