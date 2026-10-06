package com.kb.wms.inventory.application.port.in.result;

import java.time.LocalDateTime;

import com.kb.wms.inventory.domain.enums.ReferenceType;
import com.kb.wms.inventory.domain.enums.TransactionType;

/**
 * 재고 증감 이력 행 (GET /api/v1/inventory/transactions, /inventory/{inventoryId}/transactions).
 * 처리자 이름(createdByName)은 조회 쿼리가 사용자 테이블을 ID로 조인해 채운다.
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
        String createdByName,
        LocalDateTime createdAt
) {
}
