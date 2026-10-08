package com.kb.wms.inventory.application.port.in.query;

import java.time.LocalDateTime;
import java.util.List;

import com.kb.wms.inventory.domain.enums.ReferenceType;
import com.kb.wms.inventory.domain.enums.TransactionType;

/**
 * GET /api/v1/inventory/transactions, GET /api/v1/inventory/{inventoryId}/transactions 검색 조건.
 *
 * @param createdFrom 이 시각 이후
 * @param createdTo   이 시각 이전
 * @param warehouseIds 조회를 허용할 창고 범위(담당 창고). null이면 제한 없음
 */
public record InventoryTransactionSearchCondition(
        Long inventoryLotId,
        Long warehouseId,
        Long sectionId,
        Long skuId,
        Long lotId,
        TransactionType transactionType,
        ReferenceType referenceType,
        Long referenceId,
        LocalDateTime createdFrom,
        LocalDateTime createdTo,
        List<Long> warehouseIds
) {
    public InventoryTransactionSearchCondition(Long inventoryLotId, Long warehouseId, Long sectionId, Long skuId,
                                               Long lotId, TransactionType transactionType,
                                               ReferenceType referenceType, Long referenceId,
                                               LocalDateTime createdFrom, LocalDateTime createdTo) {
        this(inventoryLotId, warehouseId, sectionId, skuId, lotId, transactionType, referenceType, referenceId,
                createdFrom, createdTo, null);
    }
}
