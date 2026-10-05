package com.kb.wms.inventory.application.port.in.result;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 로트 상세의 입고 이력 한 건(GET /api/v1/lots/{lotId}의 inbounds[]).
 * InboundLine.lot_id가 이 로트인 항목을 입고(Inbound) 헤더와 조인한 읽기 전용 결과이며,
 * 입고 완료(COMPLETED)된 입고만 담는다. receivedAt은 입고 완료 일시다.
 */
public record LotInboundView(
        Long inboundId,
        String inboundNo,
        Long warehouseId,
        LocalDateTime receivedAt,
        Long receivedQuantity,
        Long acceptedQuantity,
        Long defectiveQuantity,
        BigDecimal receivedUnitPrice
) {
}
