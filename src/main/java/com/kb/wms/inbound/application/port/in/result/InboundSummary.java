package com.kb.wms.inbound.application.port.in.result;

import java.time.LocalDateTime;

import com.kb.wms.inbound.domain.enums.InboundStatus;

/**
 * 입고 목록 항목 (GET /api/v1/inbounds). 검수 항목은 개수(lineCount)만 담으며 검수 전(ARRIVED)에는 0이다.
 * 공급처는 입고가 연결된 공급처 발주서의 공급처다.
 * 처리자 이름(receivedByName)은 조회 쿼리가 사용자 테이블을 ID로 조인해 채우며, 검수 전이거나 취소된 입고는 null이다.
 */
public record InboundSummary(
        Long inboundId,
        String inboundNo,
        Long purchaseOrderId,
        String purchaseOrderNo,
        Long supplierOrderId,
        String supplierOrderNo,
        Long supplierId,
        String supplierName,
        Long warehouseId,
        String warehouseName,
        InboundStatus status,
        LocalDateTime arrivedAt,
        LocalDateTime receivedAt,
        Long receivedBy,
        String receivedByName,
        Long lineCount
) {
}
