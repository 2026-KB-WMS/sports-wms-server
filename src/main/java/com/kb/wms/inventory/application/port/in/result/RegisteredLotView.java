package com.kb.wms.inventory.application.port.in.result;

import java.math.BigDecimal;

/**
 * 로트를 찾았거나 새로 만든 결과. 엔티티를 밖으로 내보내지 않는다.
 */
public record RegisteredLotView(
        Long lotId,
        String lotNumber,
        BigDecimal unitCost
) {
}
