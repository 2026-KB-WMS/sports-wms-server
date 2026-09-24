package com.kb.wms.inventory.adapter.in.web.dto.response;

import java.time.LocalDateTime;

import com.kb.wms.inventory.application.port.in.result.InventoryTransactionView;
import com.kb.wms.inventory.domain.enums.ReferenceType;
import com.kb.wms.inventory.domain.enums.TransactionType;

/**
 * GET /api/v1/inventory/transactions, GET /api/v1/inventory/{inventoryId}/transactions 목록 항목.
 * createdByName은 User(회원) 테이블이 생기면 채운다. 지금은 createdBy(ID)만 내려준다.
 */
public record InventoryTransactionResponse(
        Long transactionId,
        Long inventoryLotId,
        Long warehouseId,
        Long sectionId,
        String sectionCode,
        Long skuId,
        String skuCode,
        Long lotId,
        String lotNumber,
        TransactionType transactionType,
        Long quantityDelta,
        Long beforeQuantity,
        Long afterQuantity,
        ReferenceType referenceType,
        Long referenceId,
        String reason,
        Long createdBy,
        LocalDateTime createdAt
) {

    public static InventoryTransactionResponse from(InventoryTransactionView view) {
        return new InventoryTransactionResponse(
                view.transactionId(),
                view.inventoryLotId(),
                view.warehouseId(),
                view.sectionId(),
                view.sectionCode(),
                view.skuId(),
                view.skuCode(),
                view.lotId(),
                view.lotNumber(),
                view.transactionType(),
                view.quantityDelta(),
                view.beforeQuantity(),
                view.afterQuantity(),
                view.referenceType(),
                view.referenceId(),
                view.reason(),
                view.createdBy(),
                view.createdAt());
    }
}
