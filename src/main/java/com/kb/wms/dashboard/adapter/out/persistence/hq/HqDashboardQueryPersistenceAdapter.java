package com.kb.wms.dashboard.adapter.out.persistence.hq;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Component;

import com.kb.wms.dashboard.application.port.in.hq.result.FlowCount;
import com.kb.wms.dashboard.application.port.in.hq.result.InventoryTotals;
import com.kb.wms.dashboard.application.port.in.hq.result.MonthlyFlow;
import com.kb.wms.dashboard.application.port.in.hq.result.PendingCounts;
import com.kb.wms.dashboard.application.port.in.hq.result.TopOutboundSku;
import com.kb.wms.dashboard.application.port.in.hq.result.WarehouseDashboardRow;
import com.kb.wms.dashboard.application.port.in.hq.result.WarehouseFlowQuantity;
import com.kb.wms.dashboard.application.port.out.hq.HqDashboardQueryRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

/**
 * 대시보드 집계 조회(읽기 전용). 다른 도메인 엔티티를 클래스로 참조하지 않고 JPQL에서 이름과 enum 상수로만
 * 조인한다(ADR-007, ADR-033). 상태 상수는 JPQL 문자열 안의 완전한 이름으로 쓴다.
 */
@Component
public class HqDashboardQueryPersistenceAdapter implements HqDashboardQueryRepository {

    private static final String STORE_ORDER_STATUS = "com.kb.wms.storeorder.domain.enums.StoreOrderStatus.";
    private static final String PURCHASE_ORDER_STATUS = "com.kb.wms.inbound.domain.enums.PurchaseOrderStatus.";
    private static final String INBOUND_STATUS = "com.kb.wms.inbound.domain.enums.InboundStatus.";
    private static final String OUTBOUND_STATUS = "com.kb.wms.outbound.domain.enums.OutboundStatus.";
    private static final String LOT_STATUS = "com.kb.wms.inventory.domain.enums.LotStatus.";
    private static final String QUALITY_STATUS = "com.kb.wms.inventory.domain.enums.QualityStatus.";
    private static final String PRODUCT_STATUS = "com.kb.wms.product.domain.enums.ProductStatus.";
    private static final String WAREHOUSE_STATUS = "com.kb.wms.warehouse.domain.enums.WarehouseStatus.";

    private static final String OUTBOUND_IN_PROGRESS = OUTBOUND_STATUS + "PICKING, "
            + OUTBOUND_STATUS + "PICKED, " + OUTBOUND_STATUS + "SHIPPED";

    /** 재고 합계에서 빼는 로트 상태(격리·폐기·만료). */
    private static final String EXCLUDED_LOT_STATUSES = LOT_STATUS + "QUARANTINED, " + LOT_STATUS + "DISPOSED, "
            + LOT_STATUS + "EXPIRED";

    /** 정상 재고 조건: 격리·폐기 로트가 아니고 품질 상태가 가용인 재고. 쿼리의 il(재고), l(로트) 별칭을 쓴다. */
    private static final String SOUND_STOCK = "l.status not in (" + EXCLUDED_LOT_STATUSES + ")"
            + " and il.qualityStatus = " + QUALITY_STATUS + "AVAILABLE";

    /**
     * 출고 한 항목의 집계 수량. 피킹 완료 전(PICKING)은 shippedQuantity가 아직 0이라 할당 수량을 쓴다.
     * 쿼리는 outbound_line(ol)과 stock_allocation(sa)을 left join한 상태여야 한다.
     */
    private static final String OUTBOUND_QUANTITY = "case when o.status = " + OUTBOUND_STATUS + "PICKING"
            + " then coalesce(sa.allocatedQuantity, 0L) else coalesce(ol.shippedQuantity, 0L) end";

    private static final String OUTBOUND_FROM = """
            from OutboundJpaEntity o
            join StoreOrderJpaEntity so on so.storeOrderId = o.storeOrderId
            left join OutboundLineJpaEntity ol on ol.outboundId = o.outboundId
            left join StockAllocationJpaEntity sa on sa.allocationId = ol.allocationId
            """;

    private static final String INBOUND_FROM = """
            from InboundJpaEntity i
            left join InboundLineJpaEntity il on il.inboundId = i.inboundId
            """;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public PendingCounts findPendingCounts() {
        return new PendingCounts(
                count("select count(so) from StoreOrderJpaEntity so where so.status = "
                        + STORE_ORDER_STATUS + "REQUESTED"),
                count("select count(po) from PurchaseOrderJpaEntity po where po.status = "
                        + PURCHASE_ORDER_STATUS + "REQUESTED"),
                count("select count(so) from StoreOrderJpaEntity so where so.status = "
                        + STORE_ORDER_STATUS + "APPROVED"));
    }

    @Override
    public InventoryTotals findInventoryTotals(LocalDate expiringOnOrBefore) {
        // 로트 상태가 우선이라 한 재고가 두 분류에 겹쳐 잡히지 않는다(불량은 격리·폐기·만료 로트 밖에서만 센다).
        Long totalQuantity = onHandSum(SOUND_STOCK);
        Long quarantinedQuantity = onHandSum("l.status = " + LOT_STATUS + "QUARANTINED");
        Long disposedQuantity = onHandSum("l.status = " + LOT_STATUS + "DISPOSED");
        Long expiredQuantity = onHandSum("l.status = " + LOT_STATUS + "EXPIRED");
        Long defectiveQuantity = onHandSum("l.status not in (" + EXCLUDED_LOT_STATUSES + ")"
                + " and il.qualityStatus = " + QUALITY_STATUS + "DEFECTIVE");
        Long activeSkuCount = count("select count(s) from ProductSkuJpaEntity s where s.status = "
                + PRODUCT_STATUS + "ACTIVE");
        Long expiringLotCount = entityManager.createQuery("""
                select count(l) from LotJpaEntity l
                where l.status = %s
                  and l.expiryDate is not null and l.expiryDate <= :limit
                  and exists (select 1 from InventoryLotJpaEntity il
                              where il.lotId = l.lotId and il.onHandQuantity > 0)
                """.formatted(LOT_STATUS + "AVAILABLE"), Long.class)
                .setParameter("limit", expiringOnOrBefore)
                .getSingleResult();
        return new InventoryTotals(totalQuantity, quarantinedQuantity, disposedQuantity, expiredQuantity,
                defectiveQuantity, activeSkuCount, loadLowStockBasis().lowStockSkuCount(), expiringLotCount);
    }

    private Long onHandSum(String condition) {
        return count("select coalesce(sum(il.onHandQuantity), 0L) from InventoryLotJpaEntity il"
                + " join LotJpaEntity l on l.lotId = il.lotId where " + condition);
    }

    @Override
    public FlowCount findInboundCompleted(YearMonth month, Long warehouseId) {
        String jpql = "select count(distinct i.inboundId), coalesce(sum(il.receivedQuantity), 0L) " + INBOUND_FROM
                + "where i.status = " + INBOUND_STATUS + "COMPLETED"
                + " and i.receivedAt >= :from and i.receivedAt < :to" + warehouseFilter("i.warehouseId", warehouseId);
        return flowCount(jpql, month, warehouseId);
    }

    @Override
    public FlowCount findInboundInProgress(Long warehouseId) {
        String jpql = "select count(distinct i.inboundId), coalesce(sum(il.receivedQuantity), 0L) " + INBOUND_FROM
                + "where i.status = " + INBOUND_STATUS + "INSPECTING" + warehouseFilter("i.warehouseId", warehouseId);
        return flowCount(jpql, null, warehouseId);
    }

    @Override
    public FlowCount findOutboundCompleted(YearMonth month, Long warehouseId) {
        String jpql = "select count(distinct o.outboundId), coalesce(sum(" + OUTBOUND_QUANTITY + "), 0L) "
                + OUTBOUND_FROM + "where o.status = " + OUTBOUND_STATUS + "DELIVERED"
                + " and o.deliveredAt >= :from and o.deliveredAt < :to" + warehouseFilter("so.warehouseId", warehouseId);
        return flowCount(jpql, month, warehouseId);
    }

    @Override
    public FlowCount findOutboundInProgress(Long warehouseId) {
        String jpql = "select count(distinct o.outboundId), coalesce(sum(" + OUTBOUND_QUANTITY + "), 0L) "
                + OUTBOUND_FROM + "where o.status in (" + OUTBOUND_IN_PROGRESS + ")"
                + warehouseFilter("so.warehouseId", warehouseId);
        return flowCount(jpql, null, warehouseId);
    }

    @Override
    public Map<String, Long> countStoreOrdersByStatus(YearMonth month) {
        List<Object[]> rows = entityManager.createQuery("""
                select so.status, count(so) from StoreOrderJpaEntity so
                where so.requestedAt >= :from and so.requestedAt < :to
                group by so.status
                """, Object[].class)
                .setParameter("from", start(month))
                .setParameter("to", end(month))
                .getResultList();
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (Object[] row : rows) {
            byStatus.put(((Enum<?>) row[0]).name(), longOf(row[1]));
        }
        return byStatus;
    }

    @Override
    public List<WarehouseDashboardRow> findWarehouseRows(YearMonth month) {
        List<Object[]> warehouses = entityManager.createQuery(
                "select w.warehouseId, w.name from WarehouseJpaEntity w where w.status = "
                        + WAREHOUSE_STATUS + "ACTIVE order by w.warehouseId", Object[].class)
                .getResultList();

        Map<Long, Long> inventoryByWarehouse = new HashMap<>();
        for (Object[] row : entityManager.createQuery("""
                select ws.warehouseId, coalesce(sum(il.onHandQuantity), 0L)
                from InventoryLotJpaEntity il
                join LotJpaEntity l on l.lotId = il.lotId
                join WarehouseSectionJpaEntity ws on ws.sectionId = il.sectionId
                where %s
                group by ws.warehouseId
                """.formatted(SOUND_STOCK), Object[].class).getResultList()) {
            inventoryByWarehouse.put(longOf(row[0]), longOf(row[1]));
        }

        Map<Long, long[]> inbound = flowByWarehouse("select i.warehouseId, i.status, coalesce(sum(il.receivedQuantity), 0L) "
                + INBOUND_FROM + "where (i.status = " + INBOUND_STATUS + "COMPLETED"
                + " and i.receivedAt >= :from and i.receivedAt < :to) or i.status = " + INBOUND_STATUS + "INSPECTING"
                + " group by i.warehouseId, i.status", month);
        Map<Long, long[]> outbound = flowByWarehouse("select so.warehouseId, o.status, coalesce(sum("
                + OUTBOUND_QUANTITY + "), 0L) " + OUTBOUND_FROM + "where (o.status = " + OUTBOUND_STATUS + "DELIVERED"
                + " and o.deliveredAt >= :from and o.deliveredAt < :to) or o.status in (" + OUTBOUND_IN_PROGRESS + ")"
                + " group by so.warehouseId, o.status", month);

        LowStockBasis lowStock = loadLowStockBasis();
        List<WarehouseDashboardRow> result = new ArrayList<>();
        for (Object[] row : warehouses) {
            Long warehouseId = longOf(row[0]);
            result.add(new WarehouseDashboardRow(warehouseId, (String) row[1],
                    inventoryByWarehouse.getOrDefault(warehouseId, 0L),
                    lowStock.lowStockSkuCount(warehouseId),
                    toQuantity(inbound.get(warehouseId)),
                    toQuantity(outbound.get(warehouseId))));
        }
        return result;
    }

    @Override
    public List<MonthlyFlow> findMonthlyCompleted(YearMonth from, YearMonth to, Long warehouseId) {
        Map<YearMonth, long[]> byMonth = new TreeMap<>();
        accumulateMonthly(byMonth, 0, "select year(i.receivedAt), month(i.receivedAt), "
                + "coalesce(sum(il.receivedQuantity), 0L) " + INBOUND_FROM + "where i.status = "
                + INBOUND_STATUS + "COMPLETED and i.receivedAt >= :from and i.receivedAt < :to"
                + warehouseFilter("i.warehouseId", warehouseId)
                + " group by year(i.receivedAt), month(i.receivedAt)", from, to, warehouseId);
        accumulateMonthly(byMonth, 1, "select year(o.deliveredAt), month(o.deliveredAt), coalesce(sum("
                + OUTBOUND_QUANTITY + "), 0L) " + OUTBOUND_FROM + "where o.status = " + OUTBOUND_STATUS
                + "DELIVERED and o.deliveredAt >= :from and o.deliveredAt < :to"
                + warehouseFilter("so.warehouseId", warehouseId)
                + " group by year(o.deliveredAt), month(o.deliveredAt)", from, to, warehouseId);
        List<MonthlyFlow> result = new ArrayList<>();
        byMonth.forEach((month, quantities) ->
                result.add(new MonthlyFlow(month, quantities[0], quantities[1], null, null)));
        return result;
    }

    @Override
    public List<TopOutboundSku> findTopOutboundSkus(YearMonth month, Long warehouseId, int limit) {
        String jpql = """
                select l.skuId, s.skuCode, s.name, sum(ol.shippedQuantity)
                from OutboundJpaEntity o
                join StoreOrderJpaEntity so on so.storeOrderId = o.storeOrderId
                join OutboundLineJpaEntity ol on ol.outboundId = o.outboundId
                join StockAllocationJpaEntity sa on sa.allocationId = ol.allocationId
                join StoreOrderLineJpaEntity l on l.storeOrderLineId = sa.storeOrderLineId
                join ProductSkuJpaEntity s on s.skuId = l.skuId
                where o.status = %s and o.deliveredAt >= :from and o.deliveredAt < :to
                """.formatted(OUTBOUND_STATUS + "DELIVERED")
                + warehouseFilter("so.warehouseId", warehouseId)
                + " group by l.skuId, s.skuCode, s.name order by sum(ol.shippedQuantity) desc, l.skuId asc";
        TypedQuery<Object[]> query = entityManager.createQuery(jpql, Object[].class)
                .setParameter("from", start(month))
                .setParameter("to", end(month))
                .setMaxResults(limit);
        bindWarehouse(query, warehouseId);
        List<TopOutboundSku> result = new ArrayList<>();
        for (Object[] row : query.getResultList()) {
            result.add(new TopOutboundSku(longOf(row[0]), (String) row[1], (String) row[2], longOf(row[3])));
        }
        return result;
    }

    // ---------- 내부 헬퍼 ----------

    private Long count(String jpql) {
        return longOf(entityManager.createQuery(jpql, Long.class).getSingleResult());
    }

    private FlowCount flowCount(String jpql, YearMonth month, Long warehouseId) {
        TypedQuery<Object[]> query = entityManager.createQuery(jpql, Object[].class);
        if (month != null) {
            query.setParameter("from", start(month)).setParameter("to", end(month));
        }
        bindWarehouse(query, warehouseId);
        Object[] row = query.getSingleResult();
        return new FlowCount(longOf(row[0]), longOf(row[1]));
    }

    /** 창고별 (완료, 진행중) 수량. 쿼리는 (창고 ID, 상태, 수량)을 돌려줘야 한다. */
    private Map<Long, long[]> flowByWarehouse(String jpql, YearMonth month) {
        Map<Long, long[]> byWarehouse = new HashMap<>();
        for (Object[] row : entityManager.createQuery(jpql, Object[].class)
                .setParameter("from", start(month))
                .setParameter("to", end(month))
                .getResultList()) {
            String status = ((Enum<?>) row[1]).name();
            long[] quantities = byWarehouse.computeIfAbsent(longOf(row[0]), k -> new long[2]);
            quantities[status.equals("COMPLETED") || status.equals("DELIVERED") ? 0 : 1] += longOf(row[2]);
        }
        return byWarehouse;
    }

    private void accumulateMonthly(Map<YearMonth, long[]> byMonth, int index, String jpql,
                                   YearMonth from, YearMonth to, Long warehouseId) {
        TypedQuery<Object[]> query = entityManager.createQuery(jpql, Object[].class)
                .setParameter("from", start(from))
                .setParameter("to", end(to));
        bindWarehouse(query, warehouseId);
        for (Object[] row : query.getResultList()) {
            YearMonth month = YearMonth.of(((Number) row[0]).intValue(), ((Number) row[1]).intValue());
            byMonth.computeIfAbsent(month, k -> new long[2])[index] += longOf(row[2]);
        }
    }

    /**
     * 안전 재고 미만 SKU를 센다. GET /inventory/low-stock과 같은 조건(활성 SKU, 안전 재고 &gt; 0,
     * 가용 = 품질·로트 상태가 모두 AVAILABLE인 행의 on_hand - allocated, 가용 &lt; 안전 재고)이며
     * 재고가 전혀 없는 SKU도 가용 0으로 센다. SKU와 창고의 모든 조합을 JPQL로 만들기 어려워 두 쿼리의
     * 결과를 메모리에서 맞춘다.
     */
    private LowStockBasis loadLowStockBasis() {
        Map<Long, Long> safety = new HashMap<>();
        for (Object[] row : entityManager.createQuery(
                "select s.skuId, s.safetyStockQuantity from ProductSkuJpaEntity s where s.status = "
                        + PRODUCT_STATUS + "ACTIVE and s.safetyStockQuantity > 0", Object[].class)
                .getResultList()) {
            safety.put(longOf(row[0]), longOf(row[1]));
        }
        Map<Long, Map<Long, Long>> availableByWarehouse = new HashMap<>();
        for (Object[] row : entityManager.createQuery("""
                select ws.warehouseId, l.skuId, coalesce(sum(il.onHandQuantity - il.allocatedQuantity), 0L)
                from InventoryLotJpaEntity il
                join LotJpaEntity l on l.lotId = il.lotId
                join WarehouseSectionJpaEntity ws on ws.sectionId = il.sectionId
                where il.qualityStatus = %s and l.status = %s
                group by ws.warehouseId, l.skuId
                """.formatted(QUALITY_STATUS + "AVAILABLE", LOT_STATUS + "AVAILABLE"), Object[].class)
                .getResultList()) {
            availableByWarehouse.computeIfAbsent(longOf(row[0]), k -> new HashMap<>())
                    .put(longOf(row[1]), longOf(row[2]));
        }
        return new LowStockBasis(safety, availableByWarehouse);
    }

    private record LowStockBasis(Map<Long, Long> safetyBySku, Map<Long, Map<Long, Long>> availableByWarehouse) {

        /** 전 창고 합산 가용 재고가 안전 재고 미만인 SKU 수. */
        long lowStockSkuCount() {
            Map<Long, Long> totalBySku = new HashMap<>();
            availableByWarehouse.values().forEach(skus -> skus.forEach((skuId, available) ->
                    totalBySku.merge(skuId, available, Long::sum)));
            return safetyBySku.entrySet().stream()
                    .filter(e -> totalBySku.getOrDefault(e.getKey(), 0L) < e.getValue())
                    .count();
        }

        /** 한 창고의 가용 재고가 안전 재고 미만인 SKU 수(창고 필터를 준 low-stock 조회와 같다). */
        long lowStockSkuCount(Long warehouseId) {
            Map<Long, Long> available = availableByWarehouse.getOrDefault(warehouseId, Map.of());
            return safetyBySku.entrySet().stream()
                    .filter(e -> available.getOrDefault(e.getKey(), 0L) < e.getValue())
                    .count();
        }
    }

    private static String warehouseFilter(String column, Long warehouseId) {
        return warehouseId == null ? "" : " and " + column + " = :warehouseId";
    }

    private static void bindWarehouse(TypedQuery<?> query, Long warehouseId) {
        if (warehouseId != null) {
            query.setParameter("warehouseId", warehouseId);
        }
    }

    private static WarehouseFlowQuantity toQuantity(long[] quantities) {
        return quantities == null
                ? new WarehouseFlowQuantity(0L, 0L)
                : new WarehouseFlowQuantity(quantities[0], quantities[1]);
    }

    private static LocalDateTime start(YearMonth month) {
        return month.atDay(1).atStartOfDay();
    }

    /** 해당 월의 다음 달 1일 0시. 월 범위는 {@code >= start, < end}로 건다. */
    private static LocalDateTime end(YearMonth month) {
        return month.plusMonths(1).atDay(1).atStartOfDay();
    }

    private static Long longOf(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }
}
