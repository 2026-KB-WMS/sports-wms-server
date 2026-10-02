package com.kb.wms.inbound.adapter.in.web.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.kb.wms.inbound.application.port.in.result.InboundCompleteResult;
import com.kb.wms.inbound.domain.enums.InboundStatus;
import com.kb.wms.inbound.domain.enums.PurchaseOrderLineStatus;
import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;

/**
 * PATCH /api/v1/inbounds/{inboundId}/complete (입고 완료) 응답: 완료된 입고, 반영된 재고 행,
 * 갱신된 발주·발주 항목 상태.
 */
public record InboundCompleteResponse(
        Long inboundId,
        String inboundNo,
        InboundStatus status,
        LocalDateTime receivedAt,
        Long receivedBy,
        List<Inventory> inventory,
        PurchaseOrder purchaseOrder,
        List<PurchaseOrderLine> purchaseOrderLines
) {

    public record Inventory(
            Long inboundLineId,
            Long acceptedInventoryLotId,
            long acceptedQuantity,
            Long defectiveInventoryLotId,
            long defectiveQuantity
    ) {
    }

    public record PurchaseOrder(Long purchaseOrderId, PurchaseOrderStatus status) {
    }

    public record PurchaseOrderLine(
            Long purchaseOrderLineId,
            long expectedQuantity,
            long receivedQuantity,
            PurchaseOrderLineStatus status
    ) {
    }

    public static InboundCompleteResponse from(InboundCompleteResult result) {
        return new InboundCompleteResponse(
                result.inbound().getInboundId(), result.inbound().getInboundNo(), result.inbound().getStatus(),
                result.inbound().getReceivedAt(), result.inbound().getReceivedBy(),
                result.inventory().stream()
                        .map(item -> new Inventory(
                                item.inboundLineId(), item.acceptedInventoryLotId(), item.acceptedQuantity(),
                                item.defectiveInventoryLotId(), item.defectiveQuantity()))
                        .toList(),
                new PurchaseOrder(result.purchaseOrderId(), result.purchaseOrderStatus()),
                result.purchaseOrderLines().stream()
                        .map(line -> new PurchaseOrderLine(
                                line.purchaseOrderLineId(), line.expectedQuantity(),
                                line.receivedQuantity(), line.status()))
                        .toList());
    }
}
