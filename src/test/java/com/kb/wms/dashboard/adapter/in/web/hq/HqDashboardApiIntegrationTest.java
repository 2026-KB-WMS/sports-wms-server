package com.kb.wms.dashboard.adapter.in.web.hq;

import static com.kb.wms.common.security.TestAuth.hqAdmin;
import static com.kb.wms.common.security.TestAuth.storeOwner;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.inbound.adapter.out.persistence.entity.InboundJpaEntity;
import com.kb.wms.inbound.adapter.out.persistence.entity.InboundLineJpaEntity;
import com.kb.wms.inbound.domain.enums.InboundStatus;
import com.kb.wms.inventory.adapter.out.persistence.entity.InventoryLotJpaEntity;
import com.kb.wms.inventory.adapter.out.persistence.entity.LotJpaEntity;
import com.kb.wms.inventory.domain.enums.LotStatus;
import com.kb.wms.inventory.domain.enums.QualityStatus;
import com.kb.wms.outbound.adapter.out.persistence.entity.OutboundJpaEntity;
import com.kb.wms.outbound.adapter.out.persistence.entity.OutboundLineJpaEntity;
import com.kb.wms.outbound.adapter.out.persistence.entity.StockAllocationJpaEntity;
import com.kb.wms.outbound.domain.enums.AllocationStatus;
import com.kb.wms.outbound.domain.enums.OutboundStatus;
import com.kb.wms.product.adapter.out.persistence.entity.ProductSkuJpaEntity;
import com.kb.wms.product.domain.enums.ProductStatus;
import com.kb.wms.storeorder.adapter.out.persistence.entity.StoreOrderJpaEntity;
import com.kb.wms.storeorder.adapter.out.persistence.entity.StoreOrderLineJpaEntity;
import com.kb.wms.storeorder.domain.enums.StoreOrderLineStatus;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;
import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseJpaEntity;
import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseSectionJpaEntity;
import com.kb.wms.warehouse.domain.enums.WarehouseStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * 컨트롤러부터 서비스·실제 쿼리까지 전체를 거쳐 명세의 응답 구조가 나오는지 확인한다.
 * 집계 규칙의 경계값은 어댑터·서비스 테스트가 확인하므로 여기서는 이어 붙은 결과만 본다.
 * 기준 월은 현재 달이라 데이터도 현재 시각에 심는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:dashboard_api;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
class HqDashboardApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @PersistenceContext private EntityManager em;

    private Long warehouseId;
    private Long skuId;
    private int seq;

    @BeforeEach
    void seed() {
        LocalDateTime now = LocalDateTime.now();
        WarehouseJpaEntity warehouse = WarehouseJpaEntity.builder()
                .warehouseCode("DASH-WH").name("대시보드 창고").address("주소").contactNumber("02-123-4567")
                .totalCapacity(BigDecimal.valueOf(1000)).status(WarehouseStatus.ACTIVE).build();
        em.persist(warehouse);
        warehouseId = warehouse.getWarehouseId();
        WarehouseSectionJpaEntity section = WarehouseSectionJpaEntity.builder()
                .warehouseId(warehouseId).sectionCode("DASH-S").name("구역").sectionType("STORAGE")
                .capacity(BigDecimal.valueOf(100)).currentCapacity(BigDecimal.ZERO)
                .status(WarehouseStatus.ACTIVE).build();
        em.persist(section);

        ProductSkuJpaEntity sku = ProductSkuJpaEntity.builder()
                .productId(1L).skuCode("DASH-SKU").name("대시보드 상품").unit("EA")
                .currentPurchasePrice(BigDecimal.TEN).currentSupplyPrice(BigDecimal.TEN)
                .safetyStockQuantity(100L).status(ProductStatus.ACTIVE).build();
        em.persist(sku);
        skuId = sku.getSkuId();
        LotJpaEntity lot = LotJpaEntity.builder()
                .skuId(skuId).supplierId(1L).lotNumber("DASH-LOT").expiryDate(LocalDate.now().plusDays(10))
                .status(LotStatus.AVAILABLE).unitCost(BigDecimal.TEN).build();
        em.persist(lot);
        em.persist(InventoryLotJpaEntity.builder()
                .sectionId(section.getSectionId()).lotId(lot.getLotId()).onHandQuantity(60L).allocatedQuantity(10L)
                .qualityStatus(QualityStatus.AVAILABLE).build());

        InboundJpaEntity inbound = InboundJpaEntity.builder()
                .inboundNo("IB-DASH").purchaseOrderId(1L).warehouseId(warehouseId).status(InboundStatus.COMPLETED)
                .arrivedAt(now.minusHours(2)).receivedAt(now).build();
        em.persist(inbound);
        em.persist(InboundLineJpaEntity.builder()
                .inboundId(inbound.getInboundId()).purchaseOrderLineId(1L).lotId(lot.getLotId())
                .receivedQuantity(50L).acceptedQuantity(50L).defectiveQuantity(0L)
                .receivedUnitPrice(BigDecimal.TEN).lineAmount(BigDecimal.valueOf(500))
                .receivedAt(now).receivedBy(1L).build());

        StoreOrderJpaEntity completed = storeOrder(StoreOrderStatus.COMPLETED, now);
        storeOrder(StoreOrderStatus.REQUESTED, now);
        StoreOrderLineJpaEntity line = StoreOrderLineJpaEntity.builder()
                .storeOrderId(completed.getStoreOrderId()).skuId(skuId).requestedQuantity(40L)
                .allocatedQuantity(40L).shippedQuantity(40L).requestedUnitSupplyPrice(BigDecimal.TEN)
                .status(StoreOrderLineStatus.COMPLETED).build();
        em.persist(line);
        StockAllocationJpaEntity allocation = StockAllocationJpaEntity.builder()
                .storeOrderLineId(line.getStoreOrderLineId()).inventoryLotId(1L)
                .allocatedQuantity(40L).pickedQuantity(40L)
                .status(AllocationStatus.ALLOCATED).allocatedAt(now.minusHours(3)).allocatedBy(1L).build();
        em.persist(allocation);
        OutboundJpaEntity outbound = OutboundJpaEntity.builder()
                .outboundNo("OB-DASH").storeOrderId(completed.getStoreOrderId()).status(OutboundStatus.DELIVERED)
                .deliveredAt(now).build();
        em.persist(outbound);
        em.persist(OutboundLineJpaEntity.builder()
                .outboundId(outbound.getOutboundId()).allocationId(allocation.getAllocationId())
                .shippedQuantity(40L).build());
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("요약은 심은 데이터가 명세 구조로 이어 붙어 나온다")
    void summary() throws Exception {
        String monthOfNow = YearMonth.now().toString();
        mockMvc.perform(get("/api/v1/dashboard/hq/summary").with(hqAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.month").value(monthOfNow))
                .andExpect(jsonPath("$.data.pending.storeOrderRequestedCount").value(1))
                .andExpect(jsonPath("$.data.inventory.totalQuantity").value(60))
                .andExpect(jsonPath("$.data.inventory.lowStockSkuCount").value(1))
                .andExpect(jsonPath("$.data.inventory.expiringLotCount").value(1))
                .andExpect(jsonPath("$.data.inbound.completed.count").value(1))
                .andExpect(jsonPath("$.data.inbound.completed.quantity").value(50))
                .andExpect(jsonPath("$.data.outbound.completed.quantity").value(40))
                .andExpect(jsonPath("$.data.storeOrders.totalCount").value(2))
                .andExpect(jsonPath("$.data.storeOrders.byStatus.REJECTED").value(0))
                .andExpect(jsonPath("$.data.storeOrders.completionRate").value(0.5))
                .andExpect(jsonPath("$.data.warehouses[?(@.warehouseId==" + warehouseId + ")].inventoryQuantity")
                        .value(60))
                .andExpect(jsonPath("$.data.warehouses[?(@.warehouseId==" + warehouseId + ")].outbound.completedQuantity")
                        .value(40));
    }

    @Test
    @DisplayName("임박 기준 일수를 줄이면 임박 로트가 빠지고, page·size·sort는 무시된다")
    void summary_expiringWithinDaysAndIgnoredPaging() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/hq/summary")
                        .param("expiringWithinDays", "5").param("page", "3").param("size", "1").param("sort", "x")
                        .with(hqAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inventory.expiringLotCount").value(0));
        mockMvc.perform(get("/api/v1/dashboard/hq/summary").param("expiringWithinDays", "366").with(hqAdmin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("추이는 months개를 채우고 현재 달에만 진행중 수량을 담으며 출고 상위 SKU를 준다")
    void trends() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/hq/trends").param("months", "3")
                        .param("warehouseId", String.valueOf(warehouseId)).with(hqAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(3))
                .andExpect(jsonPath("$.data.items[0].month").value(YearMonth.now().minusMonths(2).toString()))
                .andExpect(jsonPath("$.data.items[0].inboundCompletedQuantity").value(0))
                .andExpect(jsonPath("$.data.items[0].inboundInProgressQuantity").isEmpty())
                .andExpect(jsonPath("$.data.items[2].month").value(YearMonth.now().toString()))
                .andExpect(jsonPath("$.data.items[2].inboundCompletedQuantity").value(50))
                .andExpect(jsonPath("$.data.items[2].outboundCompletedQuantity").value(40))
                .andExpect(jsonPath("$.data.items[2].inboundInProgressQuantity").value(0))
                .andExpect(jsonPath("$.data.topOutboundSkus.length()").value(1))
                .andExpect(jsonPath("$.data.topOutboundSkus[0].skuId").value(skuId))
                .andExpect(jsonPath("$.data.topOutboundSkus[0].skuCode").value("DASH-SKU"))
                .andExpect(jsonPath("$.data.topOutboundSkus[0].quantity").value(40));
    }

    @Test
    @DisplayName("없는 창고는 404, 점주는 403")
    void errors() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/hq/trends").param("warehouseId", "999999999").with(hqAdmin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("WAREHOUSE_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/dashboard/hq/summary").with(storeOwner(3L, 1L)))
                .andExpect(status().isForbidden());
    }

    private StoreOrderJpaEntity storeOrder(StoreOrderStatus status, LocalDateTime requestedAt) {
        StoreOrderJpaEntity entity = StoreOrderJpaEntity.builder()
                .orderNo("SO-DASH-" + (++seq)).storeId(1L).warehouseId(warehouseId).status(status)
                .requestedAt(requestedAt).createdBy(1L).build();
        em.persist(entity);
        return entity;
    }
}
