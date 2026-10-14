package com.kb.wms.inventory.application.port.in.result;

import com.kb.wms.inventory.domain.enums.QualityStatus;

/**
 * 재고 수량을 바꾼 뒤 다른 도메인에 돌려주는 재고 행 상태. 엔티티를 밖으로 내보내지 않는다.
 */
public record StockLotView(
        Long inventoryLotId,
        Long sectionId,
        Long lotId,
        QualityStatus qualityStatus,
        long onHandQuantity,
        long allocatedQuantity
) {
}
