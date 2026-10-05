package com.kb.wms.outbound.application.port.in.result;

/**
 * FEFO 할당 후보 재고 행. 대상 창고의 품질·로트·구역이 모두 가용인 행이며, 조회 순서가 곧 할당 우선순위다.
 *
 * @param availableQuantity 보유 - 할당 수량(양수)
 */
public record FefoStockCandidate(
        Long skuId,
        Long inventoryLotId,
        Long sectionId,
        Long lotId,
        Long availableQuantity
) {
}