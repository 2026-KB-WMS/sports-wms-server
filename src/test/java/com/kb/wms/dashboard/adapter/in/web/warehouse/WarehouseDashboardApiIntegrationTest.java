package com.kb.wms.dashboard.adapter.in.web.warehouse;

import static com.kb.wms.common.security.TestAuth.hqAdmin;
import static com.kb.wms.common.security.TestAuth.storeOwner;
import static com.kb.wms.common.security.TestAuth.warehouseManager;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.inbound.adapter.out.persistence.entity.InboundJpaEntity;
import com.kb.wms.inbound.domain.enums.InboundStatus;
import com.kb.wms.inventory.adapter.out.persistence.entity.InventoryLotJpaEntity;
import com.kb.wms.inventory.adapter.out.persistence.entity.LotJpaEntity;
import com.kb.wms.inventory.domain.enums.LotStatus;
import com.kb.wms.inventory.domain.enums.QualityStatus;
import com.kb.wms.outbound.adapter.out.persistence.entity.OutboundJpaEntity;
import com.kb.wms.outbound.domain.enums.OutboundStatus;
import com.kb.wms.product.adapter.out.persistence.entity.ProductSkuJpaEntity;
import com.kb.wms.product.domain.enums.ProductStatus;
import com.kb.wms.storeorder.adapter.out.persistence.entity.StoreOrderJpaEntity;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;
import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseJpaEntity;
import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseSectionJpaEntity;
import com.kb.wms.warehouse.domain.enums.WarehouseStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * 창고 관리자 홈을 컨트롤러부터 서비스·실제 쿼리까지 거쳐 확인한다. 두 창고(w1, w2)에 데이터를 나눠 심어
 * 담당 창고 해석과 창고 간 데이터 분리를 본다. 집계 경계값은 어댑터·서비스 테스트가 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:dashboard_api;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
class WarehouseDashboardApiIntegrationTest {

    private static final String PATH = "/api/v1/dashboard/warehouse/summary";

    @Autowired private MockMvc mockMvc;
    @PersistenceContext private EntityManager em;

    private Long w1;
    private Long w2;
    private Long[] lowSkuIds;
    private int seq;

    /**
     * w1: 입고 검수 전 1·검수 중 1(완료 1 제외), 출고 READY 1·PICKING 1·PICKED 1·SHIPPED 1(DELIVERED 1 제외),
     * 최상위 구역(수용 100, 적치 10) 아래 하위 구역(수용 40, 적치 20), 안전 재고 100인 SKU 6개가 부족.
     * w2: 입고 검수 전 1, 출고 READY 1, 구역(수용 50, 적치 50), 재고 없음.
     */
    @BeforeEach
    void seed() {
        w1 = warehouse("DASH-W1", "서울 센터");
        w2 = warehouse("DASH-W2", "부산 센터");
        Long top = section(w1, "TOP", null, 100, 10);
        section(w1, "CHILD", top, 40, 20);
        section(w2, "ONLY", null, 50, 50);

        inbound(InboundStatus.ARRIVED, w1);
        inbound(InboundStatus.INSPECTING, w1);
        inbound(InboundStatus.COMPLETED, w1);
        inbound(InboundStatus.ARRIVED, w2);
        for (OutboundStatus status : new OutboundStatus[] {OutboundStatus.READY, OutboundStatus.PICKING,
                OutboundStatus.PICKED, OutboundStatus.SHIPPED, OutboundStatus.DELIVERED}) {
            outbound(status, w1);
        }
        outbound(OutboundStatus.READY, w2);

        // 부족 폭: 10, 50, 100(재고 없음), 20, 30, 5 → 상위 5개는 3, 2, 5, 4, 1번째
        long[] available = {90, 50, -1, 80, 70, 95};
        lowSkuIds = new Long[available.length];
        LocalDate now = LocalDate.now();
        for (int i = 0; i < available.length; i++) {
            lowSkuIds[i] = sku("부족 " + i, 100L);
            if (available[i] >= 0) {
                LocalDate expiry = i == 0 ? now.plusDays(10) : i == 1 ? now.plusDays(100) : now.plusDays(400);
                stock(top, lowSkuIds[i], available[i], expiry);
            }
        }
        stock(top, sku("충분", 10L), 20L, now.plusDays(400));
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("담당 창고가 하나면 warehouseId 없이 그 창고 요약이 명세 구조로 나온다")
    void summary_singleWarehouse() throws Exception {
        mockMvc.perform(get(PATH).with(warehouseManager(2L, w1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.warehouseId").value(w1))
                .andExpect(jsonPath("$.data.warehouseName").value("서울 센터"))
                .andExpect(jsonPath("$.data.pending.inbound.arrivedCount").value(1))
                .andExpect(jsonPath("$.data.pending.inbound.inspectingCount").value(1))
                .andExpect(jsonPath("$.data.pending.outbound.readyCount").value(1))
                .andExpect(jsonPath("$.data.pending.outbound.pickingCount").value(2))
                .andExpect(jsonPath("$.data.pending.outbound.shippedCount").value(1))
                .andExpect(jsonPath("$.data.capacity.capacity").value(100))
                .andExpect(jsonPath("$.data.capacity.currentCapacity").value(30))
                .andExpect(jsonPath("$.data.capacity.availableCapacity").value(70))
                .andExpect(jsonPath("$.data.capacity.usageRate").value(0.3))
                .andExpect(jsonPath("$.data.inventory.lowStockSkuCount").value(6))
                .andExpect(jsonPath("$.data.inventory.lowStockSkus.length()").value(5))
                .andExpect(jsonPath("$.data.inventory.lowStockSkus[0].skuId").value(lowSkuIds[2]))
                .andExpect(jsonPath("$.data.inventory.lowStockSkus[0].availableQuantity").value(0))
                .andExpect(jsonPath("$.data.inventory.lowStockSkus[1].skuId").value(lowSkuIds[1]))
                .andExpect(jsonPath("$.data.inventory.lowStockSkus[4].skuId").value(lowSkuIds[0]))
                .andExpect(jsonPath("$.data.inventory.lowStockSkus[4].safetyStockQuantity").value(100))
                .andExpect(jsonPath("$.data.inventory.expiringLotCount").value(1));
    }

    @Test
    @DisplayName("담당 창고가 여럿이면 warehouseId가 필요하고, 지정한 창고 데이터만 나오며, 비담당 창고는 403")
    void summary_multipleWarehouses() throws Exception {
        mockMvc.perform(get(PATH).with(warehouseManager(2L, w1, w2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        mockMvc.perform(get(PATH).param("warehouseId", String.valueOf(w2)).with(warehouseManager(2L, w1, w2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.warehouseName").value("부산 센터"))
                .andExpect(jsonPath("$.data.pending.inbound.arrivedCount").value(1))
                .andExpect(jsonPath("$.data.pending.inbound.inspectingCount").value(0))
                .andExpect(jsonPath("$.data.pending.outbound.readyCount").value(1))
                .andExpect(jsonPath("$.data.pending.outbound.pickingCount").value(0))
                .andExpect(jsonPath("$.data.capacity.usageRate").value(1.0))
                .andExpect(jsonPath("$.data.capacity.availableCapacity").value(0))
                // w2에는 재고가 없어 안전 재고가 있는 활성 SKU 7개가 모두 부족이다
                .andExpect(jsonPath("$.data.inventory.lowStockSkuCount").value(7))
                .andExpect(jsonPath("$.data.inventory.expiringLotCount").value(0));

        mockMvc.perform(get(PATH).param("warehouseId", String.valueOf(w2)).with(warehouseManager(2L, w1)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
        mockMvc.perform(get(PATH).param("warehouseId", "999999999").with(warehouseManager(2L, w1)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("임박 기준 일수를 늘리면 임박 로트가 늘고, 범위 밖은 400, page·size·sort는 무시된다")
    void summary_expiringWithinDays() throws Exception {
        mockMvc.perform(get(PATH).param("expiringWithinDays", "365")
                        .param("page", "3").param("size", "1").param("sort", "x")
                        .with(warehouseManager(2L, w1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inventory.expiringLotCount").value(2))
                .andExpect(jsonPath("$.data.inventory.lowStockSkus.length()").value(5));
        mockMvc.perform(get(PATH).param("expiringWithinDays", "0").with(warehouseManager(2L, w1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("본사 관리자와 점주는 403")
    void nonWarehouseManager_forbidden() throws Exception {
        mockMvc.perform(get(PATH).param("warehouseId", String.valueOf(w1)).with(hqAdmin()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(PATH).with(storeOwner(3L, 1L)))
                .andExpect(status().isForbidden());
    }

    private Long warehouse(String code, String name) {
        WarehouseJpaEntity entity = WarehouseJpaEntity.builder()
                .warehouseCode(code).name(name).address("주소").contactNumber("02-123-4567")
                .totalCapacity(BigDecimal.valueOf(1000)).status(WarehouseStatus.ACTIVE).build();
        em.persist(entity);
        return entity.getWarehouseId();
    }

    private Long section(Long warehouseId, String code, Long parentSectionId, long capacity, long currentCapacity) {
        WarehouseSectionJpaEntity entity = WarehouseSectionJpaEntity.builder()
                .warehouseId(warehouseId).parentSectionId(parentSectionId).sectionCode(code + "-" + warehouseId)
                .name(code).sectionType("STORAGE")
                .capacity(BigDecimal.valueOf(capacity)).currentCapacity(BigDecimal.valueOf(currentCapacity))
                .status(WarehouseStatus.ACTIVE).build();
        em.persist(entity);
        return entity.getSectionId();
    }

    private Long sku(String name, Long safetyStock) {
        ProductSkuJpaEntity entity = ProductSkuJpaEntity.builder()
                .productId(1L).skuCode("DASH-SKU-" + (++seq)).name(name).unit("EA")
                .currentPurchasePrice(BigDecimal.TEN).currentSupplyPrice(BigDecimal.TEN)
                .safetyStockQuantity(safetyStock).status(ProductStatus.ACTIVE).build();
        em.persist(entity);
        return entity.getSkuId();
    }

    private void stock(Long sectionId, Long skuId, long onHand, LocalDate expiryDate) {
        LotJpaEntity lot = LotJpaEntity.builder()
                .skuId(skuId).supplierId(1L).lotNumber("DASH-LOT-" + (++seq)).expiryDate(expiryDate)
                .status(LotStatus.AVAILABLE).unitCost(BigDecimal.TEN).build();
        em.persist(lot);
        em.persist(InventoryLotJpaEntity.builder()
                .sectionId(sectionId).lotId(lot.getLotId()).onHandQuantity(onHand).allocatedQuantity(0L)
                .qualityStatus(QualityStatus.AVAILABLE).build());
    }

    private void inbound(InboundStatus status, Long warehouseId) {
        em.persist(InboundJpaEntity.builder()
                .inboundNo("IB-DASH-" + (++seq)).purchaseOrderId(1L).warehouseId(warehouseId).status(status)
                .arrivedAt(LocalDateTime.now()).build());
    }

    private void outbound(OutboundStatus status, Long warehouseId) {
        StoreOrderJpaEntity order = StoreOrderJpaEntity.builder()
                .orderNo("SO-DASH-" + (++seq)).storeId(1L).warehouseId(warehouseId).status(StoreOrderStatus.ASSIGNED)
                .requestedAt(LocalDateTime.now()).createdBy(1L).build();
        em.persist(order);
        em.persist(OutboundJpaEntity.builder()
                .outboundNo("OB-DASH-" + (++seq)).storeOrderId(order.getStoreOrderId()).status(status).build());
    }
}
