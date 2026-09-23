package com.kb.wms.inventory.application.port.in.result;

import java.time.LocalDateTime;

import com.kb.wms.inventory.domain.enums.ReferenceType;
import com.kb.wms.inventory.domain.enums.TransactionType;

/**
 * 재고 증감 이력 행 (GET /api/v1/inventory/transactions, /inventory/{inventoryId}/transactions).
 * 처리자 이름은 User(회원) 테이블이 생기면 추가한다.
 */
public record InventoryTransactionView(
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
}
