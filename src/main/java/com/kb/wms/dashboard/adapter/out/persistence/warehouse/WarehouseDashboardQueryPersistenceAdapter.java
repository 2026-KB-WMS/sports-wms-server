package com.kb.wms.dashboard.adapter.out.persistence.warehouse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.kb.wms.dashboard.application.port.in.warehouse.result.CapacityUsage;
import com.kb.wms.dashboard.application.port.in.warehouse.result.InboundWorkCounts;
import com.kb.wms.dashboard.application.port.in.warehouse.result.LowStockSku;
import com.kb.wms.dashboard.application.port.in.warehouse.result.OutboundWorkCounts;
import com.kb.wms.dashboard.application.port.in.warehouse.result.WarehouseInventoryAlerts;
import com.kb.wms.dashboard.application.port.out.warehouse.WarehouseDashboardQueryRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * 창고 관리자 홈 집계 조회(읽기 전용). 다른 도메인 엔티티를 클래스로 참조하지 않고 JPQL에서 이름과 enum 상수로만
 * 조인한다(ADR-007, ADR-033). 상태 상수는 JPQL 문자열 안의 완전한 이름으로 쓴다.
 */
@Component
public class WarehouseDashboardQueryPersistenceAdapter implements WarehouseDashboardQueryRepository {

    private static final String INBOUND_STATUS = "com.kb.wms.inbound.domain.enums.InboundStatus.";
    private static final String OUTBOUND_STATUS = "com.kb.wms.outbound.domain.enums.OutboundStatus.";
    private static final String LOT_STATUS = "com.kb.wms.inventory.domain.enums.LotStatus.";
    private static final String QUALITY_STATUS = "com.kb.wms.inventory.domain.enums.QualityStatus.";
    private static final String PRODUCT_STATUS = "com.kb.wms.product.domain.enums.ProductStatus.";
    private static final String WAREHOUSE_STATUS = "com.kb.wms.warehouse.domain.enums.WarehouseStatus.";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public InboundWorkCounts findInboundWorkCounts(Long warehouseId) {
        Map<String, Long> byStatus = countByStatus("""
                select i.status, count(i) from InboundJpaEntity i
                where i.warehouseId = :warehouseId and i.status in (%s, %s)
                group by i.status
                """.formatted(INBOUND_STATUS + "ARRIVED", INBOUND_STATUS + "INSPECTING"), warehouseId);
        return new InboundWorkCounts(byStatus.getOrDefault("ARRIVED", 0L), byStatus.getOrDefault("INSPECTING", 0L));
    }

    @Override
    public OutboundWorkCounts findOutboundWorkCounts(Long warehouseId) {
        // 출고에는 창고 컬럼이 없어 지점 발주의 담당 창고로 거른다.
        Map<String, Long> byStatus = countByStatus("""
                select o.status, count(o) from OutboundJpaEntity o
                join StoreOrderJpaEntity so on so.storeOrderId = o.storeOrderId
                where so.warehouseId = :warehouseId and o.status in (%s, %s, %s, %s)
                group by o.status
                """.formatted(OUTBOUND_STATUS + "READY", OUTBOUND_STATUS + "PICKING", OUTBOUND_STATUS + "PICKED",
                OUTBOUND_STATUS + "SHIPPED"), warehouseId);
        return new OutboundWorkCounts(
                byStatus.getOrDefault("READY", 0L),
                byStatus.getOrDefault("PICKING", 0L) + byStatus.getOrDefault("PICKED", 0L),
                byStatus.getOrDefault("SHIPPED", 0L));
    }

    @Override
    public CapacityUsage findCapacityUsage(Long warehouseId) {
        BigDecimal capacity = entityManager.createQuery("""
                select coalesce(sum(ws.capacity), 0) from WarehouseSectionJpaEntity ws
                where ws.warehouseId = :warehouseId and ws.parentSectionId is null and ws.status = %s
                """.formatted(WAREHOUSE_STATUS + "ACTIVE"), BigDecimal.class)
                .setParameter("warehouseId", warehouseId)
                .getSingleResult();
        BigDecimal currentCapacity = entityManager.createQuery("""
                select coalesce(sum(ws.currentCapacity), 0) from WarehouseSectionJpaEntity ws
                where ws.warehouseId = :warehouseId and ws.status = %s
                """.formatted(WAREHOUSE_STATUS + "ACTIVE"), BigDecimal.class)
                .setParameter("warehouseId", warehouseId)
                .getSingleResult();
        return CapacityUsage.of(capacity, currentCapacity);
    }

    @Override
    public WarehouseInventoryAlerts findInventoryAlerts(Long warehouseId, LocalDate expiringOnOrBefore,
                                                        int lowStockLimit) {
        List<LowStockSku> lowStock = findLowStockSkus(warehouseId);
        Long expiringLotCount = entityManager.createQuery("""
                select count(l) from LotJpaEntity l
                where l.status = %s
                  and l.expiryDate is not null and l.expiryDate <= :limit
                  and exists (select 1 from InventoryLotJpaEntity il
                              join WarehouseSectionJpaEntity ws on ws.sectionId = il.sectionId
                              where il.lotId = l.lotId and il.onHandQuantity > 0 and ws.warehouseId = :warehouseId)
                """.formatted(LOT_STATUS + "AVAILABLE"), Long.class)
                .setParameter("limit", expiringOnOrBefore)
                .setParameter("warehouseId", warehouseId)
                .getSingleResult();
        return new WarehouseInventoryAlerts(
                (long) lowStock.size(), lowStock.stream().limit(lowStockLimit).toList(), expiringLotCount);
    }

    /**
     * 이 창고에서 가용 재고가 안전 재고 미만인 SKU를 부족 폭이 큰 순서로 돌려준다(같으면 SKU ID 오름차순).
     * GET /inventory/low-stock과 같은 조건(활성 SKU, 안전 재고 &gt; 0, 가용 = 품질·로트 상태가 모두 AVAILABLE인
     * 행의 on_hand - allocated)이며 이 창고에 재고가 전혀 없는 SKU도 가용 0으로 센다.
     */
    private List<LowStockSku> findLowStockSkus(Long warehouseId) {
        Map<Long, Long> availableBySku = new HashMap<>();
        for (Object[] row : entityManager.createQuery("""
                select l.skuId, coalesce(sum(il.onHandQuantity - il.allocatedQuantity), 0L)
                from InventoryLotJpaEntity il
                join LotJpaEntity l on l.lotId = il.lotId
                join WarehouseSectionJpaEntity ws on ws.sectionId = il.sectionId
                where ws.warehouseId = :warehouseId and il.qualityStatus = %s and l.status = %s
                group by l.skuId
                """.formatted(QUALITY_STATUS + "AVAILABLE", LOT_STATUS + "AVAILABLE"), Object[].class)
                .setParameter("warehouseId", warehouseId)
                .getResultList()) {
            availableBySku.put(longOf(row[0]), longOf(row[1]));
        }
        List<LowStockSku> lowStock = new ArrayList<>();
        for (Object[] row : entityManager.createQuery(
                "select s.skuId, s.skuCode, s.name, s.safetyStockQuantity from ProductSkuJpaEntity s"
                        + " where s.status = " + PRODUCT_STATUS + "ACTIVE and s.safetyStockQuantity > 0",
                Object[].class).getResultList()) {
            Long skuId = longOf(row[0]);
            long safety = longOf(row[3]);
            long available = availableBySku.getOrDefault(skuId, 0L);
            if (available < safety) {
                lowStock.add(new LowStockSku(skuId, (String) row[1], (String) row[2], available, safety));
            }
        }
        lowStock.sort(Comparator
                .comparingLong((LowStockSku sku) -> sku.safetyStockQuantity() - sku.availableQuantity()).reversed()
                .thenComparing(LowStockSku::skuId));
        return lowStock;
    }

    private Map<String, Long> countByStatus(String jpql, Long warehouseId) {
        Map<String, Long> byStatus = new HashMap<>();
        for (Object[] row : entityManager.createQuery(jpql, Object[].class)
                .setParameter("warehouseId", warehouseId)
                .getResultList()) {
            byStatus.put(((Enum<?>) row[0]).name(), longOf(row[1]));
        }
        return byStatus;
    }

    private static Long longOf(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }
}
