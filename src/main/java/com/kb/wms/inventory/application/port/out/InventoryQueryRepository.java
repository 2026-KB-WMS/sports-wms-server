package com.kb.wms.inventory.application.port.out;

import java.util.List;
import java.util.Optional;

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

/**
 * 재고 조회 전용 아웃바운드 포트.
 *
 * <p>재고 테이블에 SKU(product_sku)·구역(warehouse_section)·창고(warehouse)를 ID로 조인해
 * 필터·집계·정렬을 DB에서 한 번에 처리한다. 다른 도메인 테이블은 읽기에만 쓰고 변경하지 않는다.
 * 페이지네이션은 공통 페이징 도입 시 추가한다(현재는 전체 목록).
 */
public interface InventoryQueryRepository {

    /** SKU 코드 오름차순 */
    List<InventorySkuSummary> findSkuSummaries(InventorySearchCondition condition);

    /** 유통기한 오름차순(없으면 뒤로), inventory_lot_id 오름차순 */
    List<InventoryLotView> findLotViews(InventoryLotSearchCondition condition);

    Optional<InventoryDetail> findDetail(Long inventoryLotId);

    /** 부족 수량 내림차순. 안전 재고 0(미설정)·비활성 SKU는 제외, 재고가 없는 SKU는 가용 0으로 포함 */
    List<LowStockItem> findLowStock(LowStockSearchCondition condition);

    /** 생성 일시 내림차순 */
    List<InventoryTransactionView> findTransactions(InventoryTransactionSearchCondition condition);

    /** 유통기한 오름차순(없으면 뒤로), lot_id 오름차순 */
    List<LotSummary> findLots(LotSearchCondition condition);

    Optional<LotSummary> findLot(Long lotId);

    /**
     * 할당 후보: 창고·SKU의 품질·로트 AVAILABLE, 활성 구역, 가용 수량 > 0인 행을 FEFO 순서로
     * (유통기한 빠른 순 → 로트 생성 순 → inventory_lot_id 순).
     */
    List<InventoryLotView> findAllocatableStocks(Long warehouseId, Long skuId);

    // ---------- 필터 ID 존재 검증 (없는 ID로 필터링하면 404) ----------

    boolean existsSku(Long skuId);

    boolean existsWarehouse(Long warehouseId);

    boolean existsSection(Long sectionId);
}
