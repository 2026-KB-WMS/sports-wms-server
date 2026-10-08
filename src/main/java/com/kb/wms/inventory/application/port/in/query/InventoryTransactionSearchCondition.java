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
    /** 담당 창고·지점 범위 제한 없이 조회하는 조건. 사용자 요청에서는 서비스가 범위를 채워 넘긴다. */
    public static InventoryTransactionSearchCondition unscoped(Long inventoryLotId, Long warehouseId, Long sectionId, Long skuId,
            Long lotId, TransactionType transactionType,
            ReferenceType referenceType, Long referenceId,
            LocalDateTime createdFrom, LocalDateTime createdTo) {
        return new InventoryTransactionSearchCondition(inventoryLotId, warehouseId, sectionId, skuId, lotId, transactionType, referenceType, referenceId, createdFrom, createdTo, null);
    }
}
