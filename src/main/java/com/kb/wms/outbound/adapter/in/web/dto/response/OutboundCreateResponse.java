package com.kb.wms.outbound.adapter.in.web.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.kb.wms.outbound.application.port.in.result.OutboundCreateResult;
import com.kb.wms.outbound.application.port.in.result.OutboundLineView;
import com.kb.wms.outbound.application.port.in.result.OutboundView;
import com.kb.wms.outbound.domain.enums.OutboundStatus;

/** POST /api/v1/outbounds 응답(201). */
public record OutboundCreateResponse(
        Long outboundId,
        String outboundNo,
        Long storeOrderId,
        String orderNo,
        OutboundStatus status,
        String note,
        int lineCount,
        List<Item> items,
        LocalDateTime createdAt
) {

    public record Item(
            Long outboundLineId,
            Long allocationId,
            Long skuId,
            String skuCode,
            String lotNumber,
            String sectionCode,
            Long allocatedQuantity,
            Long shippedQuantity
    ) {

        static Item from(OutboundLineView l) {
            return new Item(l.outboundLineId(), l.allocationId(), l.skuId(), l.skuCode(), l.lotNumber(),
                    l.sectionCode(), l.allocatedQuantity(), l.shippedQuantity());
        }
    }

    public static OutboundCreateResponse from(OutboundCreateResult result) {
        OutboundView v = result.view();
        return new OutboundCreateResponse(v.outboundId(), v.outboundNo(), v.storeOrderId(), v.orderNo(),
                v.status(), v.note(), result.lineCount(),
                result.items().stream().map(Item::from).toList(), v.createdAt());
    }
}
