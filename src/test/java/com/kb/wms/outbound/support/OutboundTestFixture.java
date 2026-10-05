package com.kb.wms.outbound.support;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.jdbc.core.JdbcTemplate;

import com.kb.wms.inventory.adapter.out.persistence.entity.InventoryLotJpaEntity;
import com.kb.wms.inventory.adapter.out.persistence.entity.LotJpaEntity;
import com.kb.wms.inventory.adapter.out.persistence.repository.InventoryLotJpaRepository;
import com.kb.wms.inventory.adapter.out.persistence.repository.LotJpaRepository;
import com.kb.wms.inventory.domain.enums.LotStatus;
import com.kb.wms.inventory.domain.enums.QualityStatus;
import com.kb.wms.product.adapter.out.persistence.entity.ProductSkuJpaEntity;
import com.kb.wms.product.adapter.out.persistence.repository.ProductSkuJpaRepository;
import com.kb.wms.product.domain.enums.ProductStatus;
import com.kb.wms.store.adapter.out.persistence.entity.StoreJpaEntity;
import com.kb.wms.store.adapter.out.persistence.repository.StoreJpaRepository;
import com.kb.wms.store.domain.enums.StoreStatus;
import com.kb.wms.storeorder.adapter.out.persistence.entity.StoreOrderJpaEntity;
import com.kb.wms.storeorder.adapter.out.persistence.entity.StoreOrderLineJpaEntity;
import com.kb.wms.storeorder.adapter.out.persistence.repository.StoreOrderJpaRepository;
import com.kb.wms.storeorder.adapter.out.persistence.repository.StoreOrderLineJpaRepository;
import com.kb.wms.storeorder.domain.enums.StoreOrderLineStatus;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;
import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseJpaEntity;
import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseSectionJpaEntity;
import com.kb.wms.warehouse.adapter.out.persistence.repository.WarehouseJpaRepository;
import com.kb.wms.warehouse.adapter.out.persistence.repository.WarehouseSectionJpaRepository;
import com.kb.wms.warehouse.domain.enums.WarehouseStatus;

/**
 * 출고 통합·동시성 테스트용 데이터 준비. 창고 1개·구역 1개·SKU 1개·재고 행 1개와 배정(ASSIGNED) 발주를 만든다.
 * {@link org.springframework.boot.test.context.TestComponent}라서 테스트가 {@code @Import}해야 쓸 수 있다.
 */
@TestComponent
public class OutboundTestFixture {

    private static final AtomicInteger SEQ = new AtomicInteger();

    /** 한 시나리오의 ID 묶음. */
    public record Scenario(Long storeId, Long warehouseId, Long skuId, Long inventoryLotId, Long orderId,
            Long orderLineId) {
    }

    @Autowired StoreJpaRepository storeJpaRepository;
    @Autowired WarehouseJpaRepository warehouseJpaRepository;
    @Autowired WarehouseSectionJpaRepository sectionJpaRepository;
    @Autowired ProductSkuJpaRepository skuJpaRepository;
    @Autowired LotJpaRepository lotJpaRepository;
    @Autowired InventoryLotJpaRepository inventoryLotJpaRepository;
    @Autowired StoreOrderJpaRepository storeOrderJpaRepository;
    @Autowired StoreOrderLineJpaRepository storeOrderLineJpaRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    /** 보유 {@code onHand}짜리 재고 행과, 그 SKU를 {@code requested}만큼 요청한 배정 발주를 만든다. */
    public Scenario create(long onHand, long requested) {
        int n = SEQ.incrementAndGet();
        Long store = storeJpaRepository.save(StoreJpaEntity.builder()
                .storeCode("ST-F" + n).name("테스트점" + n).address("주소").contactNumber("02-1234-5678")
                .status(StoreStatus.ACTIVE).build()).getStoreId();
        Long warehouse = warehouseJpaRepository.save(WarehouseJpaEntity.builder()
                .warehouseCode("WH-F" + n).name("테스트 창고" + n).address("주소")
                .totalCapacity(BigDecimal.valueOf(100000)).status(WarehouseStatus.ACTIVE).build()).getWarehouseId();
        Long section = sectionJpaRepository.save(WarehouseSectionJpaEntity.builder()
                .warehouseId(warehouse).sectionCode("S-F" + n).name("구역" + n).sectionType("STORAGE")
                .capacity(BigDecimal.valueOf(100000)).currentCapacity(BigDecimal.valueOf(onHand))
                .status(WarehouseStatus.ACTIVE).build()).getSectionId();
        Long sku = skuJpaRepository.save(ProductSkuJpaEntity.builder()
                .productId(1L).skuCode("SKU-F" + n).name("상품" + n).unit("EA")
                .safetyStockQuantity(0L).status(ProductStatus.ACTIVE).build()).getSkuId();
        Long lot = lotJpaRepository.save(LotJpaEntity.builder()
                .skuId(sku).supplierId(1L).lotNumber("LOT-F" + n).expiryDate(LocalDate.of(2026, 12, 31))
                .status(LotStatus.AVAILABLE).unitCost(BigDecimal.TEN).build()).getLotId();
        Long inventoryLot = inventoryLotJpaRepository.save(InventoryLotJpaEntity.builder()
                .sectionId(section).lotId(lot).onHandQuantity(onHand).allocatedQuantity(0L)
                .qualityStatus(QualityStatus.AVAILABLE).build()).getInventoryLotId();

        Long[] order = newOrder(store, warehouse, sku, requested);
        return new Scenario(store, warehouse, sku, inventoryLot, order[0], order[1]);
    }

    /** 같은 창고·SKU(같은 재고 행)를 요청하는 다른 배정 발주를 만들고 {@code [발주 ID, 항목 ID]}를 돌려준다. */
    public Long[] addOrder(Scenario base, long requested) {
        return newOrder(base.storeId(), base.warehouseId(), base.skuId(), requested);
    }

    private Long[] newOrder(Long store, Long warehouse, Long sku, long requested) {
        int n = SEQ.incrementAndGet();
        Long orderId = storeOrderJpaRepository.save(StoreOrderJpaEntity.builder()
                .orderNo("SO-F" + n).storeId(store).warehouseId(warehouse)
                .status(StoreOrderStatus.ASSIGNED).requestedAt(LocalDateTime.of(2026, 10, 5, 9, 0))
                .createdBy(1L).build()).getStoreOrderId();
        Long lineId = storeOrderLineJpaRepository.save(StoreOrderLineJpaEntity.builder()
                .storeOrderId(orderId).skuId(sku).requestedQuantity(requested).allocatedQuantity(0L)
                .shippedQuantity(0L).requestedUnitSupplyPrice(new BigDecimal("1000"))
                .status(StoreOrderLineStatus.REQUESTED).build()).getStoreOrderLineId();
        return new Long[] {orderId, lineId};
    }

    /** 트랜잭션을 걸지 않는 테스트가 커밋한 행을 모두 지운다. 관련 테이블 전체를 비운다. */
    public void cleanUp() {
        List.of("outbound_line", "outbound", "stock_allocation", "inventory_transaction", "status_history",
                "store_order_line", "store_order", "inventory_lot", "lot", "warehouse_section", "warehouse",
                "product_sku", "store").forEach(table -> jdbcTemplate.update("delete from " + table));
    }
}
