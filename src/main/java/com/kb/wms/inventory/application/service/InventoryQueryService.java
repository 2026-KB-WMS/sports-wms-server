package com.kb.wms.inventory.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.inventory.application.port.in.InventoryQueryUseCase;
import com.kb.wms.inventory.application.port.in.query.InventoryLotSearchCondition;
import com.kb.wms.inventory.application.port.in.query.InventorySearchCondition;
import com.kb.wms.inventory.application.port.in.query.InventoryTransactionSearchCondition;
import com.kb.wms.inventory.application.port.in.query.LowStockSearchCondition;
import com.kb.wms.inventory.application.port.in.result.InventoryDetail;
import com.kb.wms.inventory.application.port.in.result.InventoryLotView;
import com.kb.wms.inventory.application.port.in.result.InventorySkuSummary;
import com.kb.wms.inventory.application.port.in.result.InventoryTransactionView;
import com.kb.wms.inventory.application.port.in.result.LowStockItem;
import com.kb.wms.inventory.application.port.out.InventoryLotRepository;
import com.kb.wms.inventory.application.port.out.InventoryQueryRepository;
import com.kb.wms.inventory.application.port.out.LotRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryQueryService implements InventoryQueryUseCase {

    static final String INVENTORY_NOT_FOUND_MESSAGE = "재고를 찾을 수 없습니다.";
    private static final String SKU_NOT_FOUND_MESSAGE = "SKU를 찾을 수 없습니다.";
    private static final String WAREHOUSE_NOT_FOUND_MESSAGE = "창고를 찾을 수 없습니다.";
    private static final String SECTION_NOT_FOUND_MESSAGE = "구역을 찾을 수 없습니다.";
    private static final String LOT_NOT_FOUND_MESSAGE = "로트를 찾을 수 없습니다.";

    private final InventoryQueryRepository inventoryQueryRepository;
    private final InventoryLotRepository inventoryLotRepository;
    private final LotRepository lotRepository;

    @Override
    public List<InventorySkuSummary> getInventories(InventorySearchCondition condition) {
        validateSkuId(condition.skuId());
        validateWarehouseId(condition.warehouseId());
        return inventoryQueryRepository.findSkuSummaries(condition);
    }

    @Override
    public List<InventoryLotView> getInventoriesByLot(InventoryLotSearchCondition condition) {
        validateSkuId(condition.skuId());
        validateWarehouseId(condition.warehouseId());
        validateSectionId(condition.sectionId());
        return inventoryQueryRepository.findLotViews(condition);
    }

    @Override
    public InventoryDetail getInventory(Long inventoryLotId) {
        return inventoryQueryRepository.findDetail(inventoryLotId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, INVENTORY_NOT_FOUND_MESSAGE));
    }

    @Override
    public List<LowStockItem> getLowStock(LowStockSearchCondition condition) {
        validateWarehouseId(condition.warehouseId());
        return inventoryQueryRepository.findLowStock(condition);
    }

    @Override
    public List<InventoryTransactionView> getTransactions(InventoryTransactionSearchCondition condition) {
        validatePeriod(condition);
        if (condition.referenceId() != null && condition.referenceType() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "referenceId는 referenceType과 함께 지정해야 합니다.");
        }
        validateWarehouseId(condition.warehouseId());
        validateSectionId(condition.sectionId());
        validateSkuId(condition.skuId());
        validateLotId(condition.lotId());
        return inventoryQueryRepository.findTransactions(condition);
    }

    @Override
    public List<InventoryTransactionView> getTransactionsOf(Long inventoryLotId,
                                                            InventoryTransactionSearchCondition condition) {
        validatePeriod(condition);
        if (!inventoryLotRepository.existsById(inventoryLotId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, INVENTORY_NOT_FOUND_MESSAGE);
        }
        return inventoryQueryRepository.findTransactions(new InventoryTransactionSearchCondition(
                inventoryLotId, null, null, null, null, condition.transactionType(), null, null,
                condition.createdFrom(), condition.createdTo()));
    }

    @Override
    public List<InventoryLotView> getAllocatableStocks(Long warehouseId, Long skuId) {
        return inventoryQueryRepository.findAllocatableStocks(warehouseId, skuId);
    }

    private static void validatePeriod(InventoryTransactionSearchCondition condition) {
        if (condition.createdFrom() != null && condition.createdTo() != null
                && condition.createdFrom().isAfter(condition.createdTo())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "createdFrom은 createdTo보다 이전이어야 합니다.");
        }
    }

    private void validateSkuId(Long skuId) {
        if (skuId != null && !inventoryQueryRepository.existsSku(skuId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, SKU_NOT_FOUND_MESSAGE);
        }
    }

    private void validateWarehouseId(Long warehouseId) {
        if (warehouseId != null && !inventoryQueryRepository.existsWarehouse(warehouseId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, WAREHOUSE_NOT_FOUND_MESSAGE);
        }
    }

    private void validateSectionId(Long sectionId) {
        if (sectionId != null && !inventoryQueryRepository.existsSection(sectionId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, SECTION_NOT_FOUND_MESSAGE);
        }
    }

    private void validateLotId(Long lotId) {
        if (lotId != null && !lotRepository.existsById(lotId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, LOT_NOT_FOUND_MESSAGE);
        }
    }
}
