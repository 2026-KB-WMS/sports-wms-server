package com.kb.wms.inventory.application.port.out;

import java.util.List;
import java.util.Optional;

import com.kb.wms.common.paging.PageCondition;
import com.kb.wms.common.paging.PageResult;
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

/**
 * 재고 조회 전용 아웃바운드 포트.
 *
 * <p>재고 테이블에 SKU(product_sku)·구역(warehouse_section)·창고(warehouse)를 ID로 조인해
 * 필터·집계·정렬을 DB에서 한 번에 처리한다. 다른 도메인 테이블은 읽기에만 쓰고 변경하지 않는다.
 * 목록 조회는 {@link PageCondition}으로 정렬·페이징한 {@link PageResult}를 돌려준다. 재고별 이력·로트 상세처럼 전체를
 * 읽는 용도는 페이지 조건 없는 목록 메서드를 쓴다.
 */
public interface InventoryQueryRepository {

    PageResult<InventorySkuSummary> findSkuSummaries(InventorySearchCondition condition, PageCondition page);

    /** 보유 0 행은 제외 */
    PageResult<InventorySectionSummary> findSectionSummaries(InventorySectionSearchCondition condition, PageCondition page);

    PageResult<InventoryLotView> findLotViews(InventoryLotSearchCondition condition, PageCondition page);

    /** 페이지 없이 전체. 유통기한 오름차순(없으면 뒤로), inventory_lot_id 오름차순 */
    List<InventoryLotView> findLotViews(InventoryLotSearchCondition condition);

    Optional<InventoryDetail> findDetail(Long inventoryLotId);

    /** 부족 수량 내림차순. 안전 재고 0(미설정)·비활성 SKU는 제외, 재고가 없는 SKU는 가용 0으로 포함 */
    List<LowStockItem> findLowStock(LowStockSearchCondition condition);

    PageResult<InventoryTransactionView> findTransactions(InventoryTransactionSearchCondition condition, PageCondition page);

    /** 페이지 없이 전체. 생성 일시 내림차순, transaction_id 내림차순 */
    List<InventoryTransactionView> findTransactions(InventoryTransactionSearchCondition condition);

    PageResult<LotSummary> findLots(LotSearchCondition condition, PageCondition page);

    Optional<LotSummary> findLot(Long lotId);

    /** 로트가 warehouseIds 중 하나에 재고 또는 입고 완료 이력이 있을 때만 반환한다. */
    Optional<LotSummary> findLot(Long lotId, List<Long> warehouseIds);

    /** 로트가 입고된 이력(InboundLine.lot_id, 입고 완료 건만). 입고 완료 일시 내림차순, inbound_id 내림차순. */
    List<LotInboundView> findLotInbounds(Long lotId);

    /**
     * 할당 후보: 창고·SKU의 품질·로트 AVAILABLE, 활성 구역, 가용 수량 > 0인 행을 FEFO 순서로
     * (유통기한 빠른 순 → 로트 생성 순 → inventory_lot_id 순).
     */
    List<InventoryLotView> findAllocatableStocks(Long warehouseId, Long skuId);

    /** 구역에 보유 또는 할당 수량이 0보다 큰 재고 행이 있는지 */
    boolean existsStockInSection(Long sectionId);

    /** 창고의 구역 중 보유 또는 할당 수량이 0보다 큰 재고 행이 있는지 */
    boolean existsStockInWarehouse(Long warehouseId);

    // ---------- 필터 ID 존재 검증 (없는 ID로 필터링하면 404) ----------

    boolean existsSku(Long skuId);

    boolean existsWarehouse(Long warehouseId);

    boolean existsSection(Long sectionId);

    /** 카테고리(상품 도메인 category 테이블) 존재 여부. 읽기 전용 확인이다. */
    boolean existsCategory(Long categoryId);

    /** 구역이 속한 창고 ID. 구역이 없으면 비어 있다. */
    Optional<Long> findWarehouseIdOfSection(Long sectionId);

    /** 공급처(입고 도메인 supplier 테이블) 존재 여부. 읽기 전용 확인이며 입고 서비스를 거치지 않는다. */
    boolean existsSupplier(Long supplierId);
}
