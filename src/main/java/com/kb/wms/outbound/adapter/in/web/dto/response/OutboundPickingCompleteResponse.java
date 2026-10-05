package com.kb.wms.outbound.adapter.in.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.kb.wms.outbound.application.port.in.result.OutboundPickingCompleteResult;
import com.kb.wms.outbound.application.port.in.result.OutboundPickingCompleteResult.PickedItem;
import com.kb.wms.outbound.domain.enums.OutboundStatus;

/** PATCH /api/v1/outbounds/{outboundId}/picking/complete 응답. */
public record OutboundPickingCompleteResponse(
        Long outboundId,
        String outboundNo,
        OutboundStatus status,
        boolean hasShortage,
        List<Item> items,
        LocalDateTime updatedAt
) {

    public record Item(
            Long outboundLineId,
            Long allocationId,
            long allocatedQuantity,
            long pickedQuantity,
            long shortageQuantity,
            BigDecimal confirmedUnitSupplyPrice,
            BigDecimal lineAmount,
            Inventory inventory
    ) {

        static Item from(PickedItem i) {
            return new Item(i.outboundLineId(), i.allocationId(), i.allocatedQuantity(), i.pickedQuantity(),
                    i.shortageQuantity(), i.confirmedUnitSupplyPrice(), i.lineAmount(),
                    new Inventory(i.inventory().inventoryLotId(), i.inventory().onHandQuantity(),
                            i.inventory().allocatedQuantity()));
        }
    }

    public record Inventory(Long inventoryLotId, long onHandQuantity, long allocatedQuantity) {
    }

    public static OutboundPickingCompleteResponse from(OutboundPickingCompleteResult r) {
        return new OutboundPickingCompleteResponse(r.outboundId(), r.outboundNo(), r.status(), r.hasShortage(),
                r.items().stream().map(Item::from).toList(), r.updatedAt());
    }
}
