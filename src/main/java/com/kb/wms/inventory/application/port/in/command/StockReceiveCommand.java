package com.kb.wms.inventory.application.port.in.command;

import com.kb.wms.inventory.domain.enums.QualityStatus;

/**
 * 입고 완료 시 한 재고 행(구역 + 로트)에 보유 수량을 더한다. 행이 없으면 만든다.
 *
 * @param qualityStatus 합격품은 AVAILABLE, 불량품은 DEFECTIVE
 * @param inboundId     InventoryTransaction.reference_id
 * @param userId        처리 사용자
 */
public record StockReceiveCommand(
        Long sectionId,
        Long lotId,
        QualityStatus qualityStatus,
        long quantity,
        Long inboundId,
        Long userId
) {
}
