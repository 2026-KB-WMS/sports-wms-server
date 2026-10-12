package com.kb.wms.inbound.application.port.in.result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 입고 검수 항목 조회용 (GET /api/v1/inbounds/{inboundId}/details).
 * SKU·로트·구역 정보를 조인해 담고, orderedUnitPrice는 발주 항목의 단가 스냅샷이다.
 * 구역이 아직 정해지지 않은 항목의 구역 ID·코드는 null이다. 처리자 이름(receivedByName)은 사용자 테이블을 ID로 조인해 채운다.
 */
public record InboundLineView(
        Long inboundLineId,
        Long purchaseOrderLineId,
        Long skuId,
        String skuCode,
        String skuName,
        Long lotId,
        String lotNumber,
        LocalDate manufacturedDate,
        LocalDate expiryDate,
        Long receivedQuantity,
        Long acceptedQuantity,
        Long defectiveQuantity,
        Long acceptedSectionId,
        String acceptedSectionCode,
        Long defectSectionId,
        String defectSectionCode,
        BigDecimal orderedUnitPrice,
        BigDecimal receivedUnitPrice,
        BigDecimal lineAmount,
        String priceChangeReason,
        String inspectionNote,
        LocalDateTime receivedAt,
        Long receivedBy,
        String receivedByName
) {
}
