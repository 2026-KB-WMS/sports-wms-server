package com.kb.wms.inbound.application.port.in.result;

import java.time.LocalDateTime;

import com.kb.wms.inbound.domain.enums.InboundStatus;
import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;

/**
 * 입고 헤더 단건 (GET /api/v1/inbounds/{inboundId}): 입고 헤더 + 연결된 발주·공급처·창고 정보.
 * 취소 사유는 조회 쿼리에서 읽지 않고, 서비스가 CANCELED 입고에 대해 StatusHistory에서 읽어
 * {@link #withCancelReason(String)}으로 채운다. 처리자 이름(회원 도메인)은 해당 도메인이 생기면 추가한다.
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
        LocalDateTime updatedAt,
        String cancelReason
) {

    /** 조회 쿼리(JPQL 생성자 표현식)용: 취소 사유 없이 만든다. */
    public InboundView(Long inboundId, String inboundNo, Long purchaseOrderId, String purchaseOrderNo,
                       PurchaseOrderStatus purchaseOrderStatus, Long supplierId, String supplierName,
                       Long warehouseId, String warehouseName, InboundStatus status, LocalDateTime arrivedAt,
                       LocalDateTime receivedAt, Long receivedBy, String note, Long lineCount,
                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(inboundId, inboundNo, purchaseOrderId, purchaseOrderNo, purchaseOrderStatus, supplierId, supplierName,
                warehouseId, warehouseName, status, arrivedAt, receivedAt, receivedBy, note, lineCount,
                createdAt, updatedAt, null);
    }

    public InboundView withCancelReason(String cancelReason) {
        return new InboundView(inboundId, inboundNo, purchaseOrderId, purchaseOrderNo, purchaseOrderStatus,
                supplierId, supplierName, warehouseId, warehouseName, status, arrivedAt, receivedAt, receivedBy,
                note, lineCount, createdAt, updatedAt, cancelReason);
    }
}
