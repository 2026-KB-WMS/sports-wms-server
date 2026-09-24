package com.kb.wms.inventory.adapter.in.web.dto.response;

import java.time.LocalDateTime;

import com.kb.wms.inventory.application.port.in.result.InventoryAdjustmentResult;
import com.kb.wms.inventory.domain.entity.InventoryLot;
import com.kb.wms.inventory.domain.entity.InventoryTransaction;
import com.kb.wms.inventory.domain.enums.TransactionType;

/**
 * POST /api/v1/inventory/adjustments 응답.
 */
public record InventoryAdjustmentResponse(
        Long transactionId,
        Long inventoryLotId,
        TransactionType transactionType,
        Long quantityDelta,
        Long beforeQuantity,
        Long afterQuantity,
        String reason,
        Long createdBy,
        LocalDateTime createdAt,
        InventorySnapshot inventory
) {

    public record InventorySnapshot(
            Long onHandQuantity,
            Long allocatedQuantity,
            Long availableQuantity
    ) {
    }

    public static InventoryAdjustmentResponse from(InventoryAdjustmentResult result) {
        InventoryTransaction transaction = result.transaction();
        InventoryLot inventoryLot = result.inventoryLot();
        return new InventoryAdjustmentResponse(
                transaction.getTransactionId(),
                transaction.getInventoryLotId(),
                transaction.getTransactionType(),
                transaction.getQuantityDelta(),
                transaction.getBeforeQuantity(),
                transaction.getAfterQuantity(),
                transaction.getReason(),
                transaction.getCreatedBy(),
                transaction.getCreatedAt(),
                new InventorySnapshot(
                        inventoryLot.getOnHandQuantity(),
                        inventoryLot.getAllocatedQuantity(),
                        inventoryLot.availableQuantity()));
    }
}
