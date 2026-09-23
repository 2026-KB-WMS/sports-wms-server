package com.kb.wms.inventory.application.port.in;

import java.util.List;

import com.kb.wms.inventory.application.port.in.query.InventoryLotSearchCondition;
import com.kb.wms.inventory.application.port.in.query.InventorySearchCondition;
import com.kb.wms.inventory.application.port.in.query.InventoryTransactionSearchCondition;
import com.kb.wms.inventory.application.port.in.query.LowStockSearchCondition;
import com.kb.wms.inventory.application.port.in.result.InventoryDetail;
import com.kb.wms.inventory.application.port.in.result.InventoryLotView;
import com.kb.wms.inventory.application.port.in.result.InventorySkuSummary;
import com.kb.wms.inventory.application.port.in.result.InventoryTransactionView;
import com.kb.wms.inventory.application.port.in.result.LowStockItem;

/**
 * 재고·재고 이력 조회 유스케이스.
 * GET /api/v1/inventory, /inventory/by-lot, /inventory/{inventoryId}, /inventory/low-stock,
 * /inventory/transactions, /inventory/{inventoryId}/transactions
 */
public interface InventoryQueryUseCase {

    List<InventorySkuSummary> getInventories(InventorySearchCondition condition);

    List<InventoryLotView> getInventoriesByLot(InventoryLotSearchCondition condition);

    InventoryDetail getInventory(Long inventoryLotId);

    List<LowStockItem> getLowStock(LowStockSearchCondition condition);

    List<InventoryTransactionView> getTransactions(InventoryTransactionSearchCondition condition);

    /**
     * 특정 재고 행의 이력. 재고가 없으면 NOT_FOUND.
     */
    List<InventoryTransactionView> getTransactionsOf(Long inventoryLotId,
                                                     InventoryTransactionSearchCondition condition);

    /**
     * 출고 할당(POST /allocations)용 후보 재고 행. 창고·SKU의 가용 재고를 FEFO 순서로 반환한다.
     * 조회 결과는 잠금 전 스냅샷이므로 실제 할당은 {@link InventoryStockUseCase#allocate}에서 다시 검증된다.
     */
    List<InventoryLotView> getAllocatableStocks(Long warehouseId, Long skuId);
}
