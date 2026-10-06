package com.kb.wms.storeorder.adapter.in.web.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.kb.wms.storeorder.application.port.in.result.StoreOrderDetails;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderOutboundView;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderStatusHistoryView;
import com.kb.wms.storeorder.domain.enums.StoreOrderOutboundStatus;
import com.kb.wms.storeorder.domain.enums.StoreOrderProgressStage;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

/**
 * GET /api/v1/orders/{orderId}/details 응답. 항목·출고·상태 이력을 담는다.
 * 출고 도메인 연동 전에는 outbounds가 빈 배열이고, 처리자 이름(changedByName)은 상태 이력 패키지를 분리할 때 채우므로 아직 null이다.
 */
public record StoreOrderDetailsResponse(
        Long storeOrderId,
        String orderNo,
        StoreOrderStatus status,
        StoreOrderProgressStage progressStage,
        List<StoreOrderLineResponse> items,
        List<Outbound> outbounds,
        List<History> statusHistory
) {

    public record Outbound(
            Long outboundId,
            String outboundNo,
            StoreOrderOutboundStatus status,
            LocalDateTime shippedAt,
            LocalDateTime deliveredAt
    ) {

        static Outbound from(StoreOrderOutboundView outbound) {
            return new Outbound(outbound.outboundId(), outbound.outboundNo(), outbound.status(),
                    outbound.shippedAt(), outbound.deliveredAt());
        }
    }

    public record History(
            String fromStatus,
            String toStatus,
            String reason,
            Long changedBy,
            String changedByName,
            LocalDateTime changedAt
    ) {

        static History from(StoreOrderStatusHistoryView history) {
            return new History(history.fromStatus(), history.toStatus(), history.reason(),
                    history.changedBy(), null, history.changedAt());
        }
    }

    public static StoreOrderDetailsResponse from(StoreOrderDetails details) {
        return new StoreOrderDetailsResponse(
                details.storeOrderId(),
                details.orderNo(),
                details.status(),
                details.progressStage(),
                details.items().stream().map(StoreOrderLineResponse::from).toList(),
                details.outbounds().stream().map(Outbound::from).toList(),
                details.statusHistory().stream().map(History::from).toList());
    }
}
