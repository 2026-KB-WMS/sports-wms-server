package com.kb.wms.inventory.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.kb.wms.common.paging.PageCondition;
import com.kb.wms.common.paging.PageResult;
import com.kb.wms.common.paging.SortDirection;
import com.kb.wms.common.paging.SortOrder;
import com.kb.wms.common.persistence.PageAdapter;
import com.kb.wms.inventory.adapter.out.persistence.repository.InventoryLotJpaRepository;
import com.kb.wms.inventory.adapter.out.persistence.repository.InventoryTransactionJpaRepository;
import com.kb.wms.inventory.adapter.out.persistence.repository.LotJpaRepository;
import com.kb.wms.inventory.application.port.in.query.InventoryLotSearchCondition;
import com.kb.wms.inventory.application.port.in.query.InventorySearchCondition;
import com.kb.wms.inventory.application.port.in.query.InventorySectionSearchCondition;
import com.kb.wms.inventory.application.port.in.query.InventoryTransactionSearchCondition;
import com.kb.wms.inventory.application.port.in.query.LotSearchCondition;
import com.kb.wms.inventory.application.port.in.query.LowStockSearchCondition;
import com.kb.wms.inventory.application.port.in.result.InventoryDetail;
import com.kb.wms.inventory.application.port.in.result.InventoryLotView;
import com.kb.wms.inventory.application.port.in.result.InventorySectionSummary;
import com.kb.wms.inventory.application.port.in.result.InventorySkuSummary;
import com.kb.wms.inventory.application.port.in.result.InventoryTransactionView;
import com.kb.wms.inventory.application.port.in.result.LotInboundView;
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

    /** 유통기한이 없는 행을 뒤로 보내는 정렬 식(없으면 먼 미래로 본다). 오름차순이면 임박순, 내림차순이면 먼 순. */
    private static final String EXPIRY_NULLS_LAST = "coalesce(l.expiryDate, {d '9999-12-31'})";

    @Override
    public PageResult<InventorySkuSummary> findSkuSummaries(InventorySearchCondition condition, PageCondition page) {
        return PageAdapter.toPageResult(inventoryLotJpaRepository.findSkuSummaries(
                condition.skuId(), condition.warehouseId(), scoped(condition.warehouseIds()),
                scopeIds(condition.warehouseIds()), keyword(condition.keyword()), PageAdapter.toPageable(page)), page);
    }

    @Override
    public PageResult<InventorySectionSummary> findSectionSummaries(InventorySectionSearchCondition condition,
                                                                    PageCondition page) {
        return PageAdapter.toPageResult(inventoryLotJpaRepository.findSectionSummaries(
                condition.warehouseId(), condition.sectionId(), condition.categoryId(), condition.skuId(),
                scoped(condition.warehouseIds()), scopeIds(condition.warehouseIds()), keyword(condition.keyword()),
                PageAdapter.toPageable(page)), page);
    }

    @Override
    public PageResult<InventoryLotView> findLotViews(InventoryLotSearchCondition condition, PageCondition page) {
        return PageAdapter.toPageResult(lotViewPage(condition, PageAdapter.toPageable(page)), page);
    }

    @Override
    public List<InventoryLotView> findLotViews(InventoryLotSearchCondition condition) {
        return lotViewPage(condition, PageAdapter.unpaged(
                new SortOrder("expiryDate", EXPIRY_NULLS_LAST, SortDirection.ASC),
                new SortOrder("inventoryLotId", "il.inventoryLotId", SortDirection.ASC))).getContent();
    }

    private Page<InventoryLotView> lotViewPage(InventoryLotSearchCondition condition, Pageable pageable) {
        return inventoryLotJpaRepository.findLotViews(
                condition.skuId(), condition.warehouseId(), condition.sectionId(), condition.lotId(),
                condition.expiringBefore(), condition.qualityStatus(),
                Boolean.TRUE.equals(condition.includeEmpty()),
                scoped(condition.warehouseIds()), scopeIds(condition.warehouseIds()), pageable);
    }

    @Override
    public Optional<InventoryDetail> findDetail(Long inventoryLotId) {
        return inventoryLotJpaRepository.findDetail(inventoryLotId);
    }

    @Override
    public List<LowStockItem> findLowStock(LowStockSearchCondition condition) {
        return inventoryLotJpaRepository.findLowStock(condition.warehouseId(),
                scoped(condition.warehouseIds()), scopeIds(condition.warehouseIds()), keyword(condition.keyword()));
    }

    @Override
    public PageResult<InventoryTransactionView> findTransactions(InventoryTransactionSearchCondition condition,
                                                                 PageCondition page) {
        return PageAdapter.toPageResult(transactionPage(condition, PageAdapter.toPageable(page)), page);
    }

    @Override
    public List<InventoryTransactionView> findTransactions(InventoryTransactionSearchCondition condition) {
        return transactionPage(condition, PageAdapter.unpaged(
                new SortOrder("createdAt", "t.createdAt", SortDirection.DESC),
                new SortOrder("transactionId", "t.transactionId", SortDirection.DESC))).getContent();
    }

    private Page<InventoryTransactionView> transactionPage(InventoryTransactionSearchCondition condition,
                                                           Pageable pageable) {
        return inventoryTransactionJpaRepository.findViews(
                condition.inventoryLotId(), condition.warehouseId(), scoped(condition.warehouseIds()),
                scopeIds(condition.warehouseIds()), condition.sectionId(), condition.skuId(),
                condition.lotId(), condition.transactionType(), condition.referenceType(), condition.referenceId(),
                condition.createdFrom(), condition.createdTo(), pageable);
    }

    @Override
    public PageResult<LotSummary> findLots(LotSearchCondition condition, PageCondition page) {
        return PageAdapter.toPageResult(lotJpaRepository.findSummaries(
                null, condition.skuId(), condition.supplierId(), condition.expiringBefore(),
                keyword(condition.keyword()), scoped(condition.warehouseIds()), scopeIds(condition.warehouseIds()),
                PageAdapter.toPageable(page)), page);
    }

    @Override
    public Optional<LotSummary> findLot(Long lotId, List<Long> warehouseIds) {
        return lotJpaRepository.findSummaries(lotId, null, null, null, null, scoped(warehouseIds), scopeIds(warehouseIds),
                Pageable.unpaged()).stream().findFirst();
    }

    @Override
    public Optional<Long> findWarehouseIdOfSection(Long sectionId) {
        return inventoryLotJpaRepository.findWarehouseIdBySectionId(sectionId);
    }

    @Override
    public Optional<LotSummary> findLot(Long lotId) {
        return lotJpaRepository.findSummaries(lotId, null, null, null, null, false, NO_WAREHOUSE, Pageable.unpaged())
                .stream().findFirst();
    }

    @Override
    public List<LotInboundView> findLotInbounds(Long lotId) {
        return lotJpaRepository.findInbounds(lotId);
    }

    @Override
    public List<InventoryLotView> findAllocatableStocks(Long warehouseId, Long skuId) {
        return inventoryLotJpaRepository.findAllocatableStocks(warehouseId, skuId);
    }

    @Override
    public boolean existsStockInSection(Long sectionId) {
        return inventoryLotJpaRepository.existsStockInSection(sectionId);
    }

    @Override
    public boolean existsStockInWarehouse(Long warehouseId) {
        return inventoryLotJpaRepository.existsStockInWarehouse(warehouseId);
    }

    @Override
    public boolean existsSku(Long skuId) {
        return inventoryLotJpaRepository.existsSkuId(skuId);
    }

    @Override
    public boolean existsWarehouse(Long warehouseId) {
        return inventoryLotJpaRepository.existsWarehouseId(warehouseId);
    }

    @Override
    public boolean existsSection(Long sectionId) {
        return inventoryLotJpaRepository.existsSectionId(sectionId);
    }

    @Override
    public boolean existsCategory(Long categoryId) {
        return inventoryLotJpaRepository.existsCategoryId(categoryId);
    }

    @Override
    public boolean existsSupplier(Long supplierId) {
        return inventoryLotJpaRepository.existsSupplierId(supplierId);
    }

    /** JPQL의 IN에 빈 목록을 넘기지 않으려는 자리 값. 범위 조건을 쓰지 않을 때(scoped=false)나 담당 창고가 없을 때 쓴다. */
    static final List<Long> NO_WAREHOUSE = List.of(-1L);

    private static boolean scoped(List<Long> warehouseIds) {
        return warehouseIds != null;
    }

    private static List<Long> scopeIds(List<Long> warehouseIds) {
        return warehouseIds == null || warehouseIds.isEmpty() ? NO_WAREHOUSE : warehouseIds;
    }

    /** 빈 문자열·공백 검색어는 조건 없음으로 본다. */
    private static String keyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }
}
