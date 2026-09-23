package com.kb.wms.inventory.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.kb.wms.inventory.adapter.out.persistence.repository.InventoryLotJpaRepository;
import com.kb.wms.inventory.adapter.out.persistence.repository.InventoryTransactionJpaRepository;
import com.kb.wms.inventory.adapter.out.persistence.repository.LotJpaRepository;
import com.kb.wms.inventory.application.port.in.query.InventoryLotSearchCondition;
import com.kb.wms.inventory.application.port.in.query.InventorySearchCondition;
import com.kb.wms.inventory.application.port.in.query.InventoryTransactionSearchCondition;
import com.kb.wms.inventory.application.port.in.query.LotSearchCondition;
import com.kb.wms.inventory.application.port.in.query.LowStockSearchCondition;
import com.kb.wms.inventory.application.port.in.result.InventoryDetail;
import com.kb.wms.inventory.application.port.in.result.InventoryLotView;
import com.kb.wms.inventory.application.port.in.result.InventorySkuSummary;
import com.kb.wms.inventory.application.port.in.result.InventoryTransactionView;
import com.kb.wms.inventory.application.port.in.result.LotSummary;
import com.kb.wms.inventory.application.port.in.result.LowStockItem;
import com.kb.wms.inventory.application.port.out.InventoryQueryRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InventoryQueryPersistenceAdapter implements InventoryQueryRepository {

    private final InventoryLotJpaRepository inventoryLotJpaRepository;
    private final InventoryTransactionJpaRepository inventoryTransactionJpaRepository;
    private final LotJpaRepository lotJpaRepository;

    @Override
    public List<InventorySkuSummary> findSkuSummaries(InventorySearchCondition condition) {
        return inventoryLotJpaRepository.findSkuSummaries(
                condition.skuId(), condition.warehouseId(), keyword(condition.keyword()));
    }

    @Override
    public List<InventoryLotView> findLotViews(InventoryLotSearchCondition condition) {
        return inventoryLotJpaRepository.findLotViews(
                condition.skuId(), condition.warehouseId(), condition.sectionId(), condition.lotId(),
                condition.expiringBefore(), condition.qualityStatus(),
                Boolean.TRUE.equals(condition.includeEmpty()));
    }

    @Override
    public Optional<InventoryDetail> findDetail(Long inventoryLotId) {
        return inventoryLotJpaRepository.findDetail(inventoryLotId);
    }

    @Override
    public List<LowStockItem> findLowStock(LowStockSearchCondition condition) {
        return inventoryLotJpaRepository.findLowStock(condition.warehouseId(), keyword(condition.keyword()));
    }

    @Override
    public List<InventoryTransactionView> findTransactions(InventoryTransactionSearchCondition condition) {
        return inventoryTransactionJpaRepository.findViews(
                condition.inventoryLotId(), condition.warehouseId(), condition.sectionId(), condition.skuId(),
                condition.lotId(), condition.transactionType(), condition.referenceType(), condition.referenceId(),
                condition.createdFrom(), condition.createdTo());
    }

    @Override
    public List<LotSummary> findLots(LotSearchCondition condition) {
        return lotJpaRepository.findSummaries(
                null, condition.skuId(), condition.supplierId(), condition.expiringBefore(),
                keyword(condition.keyword()));
    }

    @Override
    public Optional<LotSummary> findLot(Long lotId) {
        return lotJpaRepository.findSummaries(lotId, null, null, null, null).stream().findFirst();
    }

    @Override
    public List<InventoryLotView> findAllocatableStocks(Long warehouseId, Long skuId) {
        return inventoryLotJpaRepository.findAllocatableStocks(warehouseId, skuId);
    }

    /** 빈 문자열·공백 검색어는 조건 없음으로 본다. */
    private static String keyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }
}
