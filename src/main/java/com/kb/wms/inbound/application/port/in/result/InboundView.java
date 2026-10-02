package com.kb.wms.inbound.application.port.in.result;

import java.time.LocalDateTime;

import com.kb.wms.inbound.domain.enums.InboundStatus;
import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;

/**
 * 입고 헤더 단건 (GET /api/v1/inbounds/{inboundId}): 입고 헤더 + 연결된 발주·공급처·창고 정보.
 * 취소 사유(StatusHistory)와 처리자 이름(회원 도메인)은 해당 도메인이 생기면 추가한다.
 * purchaseOrderStatus는 취소·완료 응답에서 발주 상태를 함께 내려주기 위한 값이다.
 */
public record InboundView(
        Long inboundId,
        String inboundNo,
        Long purchaseOrderId,
        String purchaseOrderNo,
        PurchaseOrderStatus purchaseOrderStatus,
        Long supplierId,
        String supplierName,
        Long warehouseId,
        String warehouseName,
        InboundStatus status,
        LocalDateTime arrivedAt,
        LocalDateTime receivedAt,
        Long receivedBy,
        String note,
        Long lineCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
