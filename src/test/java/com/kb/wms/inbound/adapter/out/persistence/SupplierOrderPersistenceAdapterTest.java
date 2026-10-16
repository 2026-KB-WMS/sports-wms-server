package com.kb.wms.inbound.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.paging.TestPages;
import com.kb.wms.common.paging.PageResult;
import com.kb.wms.common.paging.SortDirection;
import com.kb.wms.common.paging.PageCondition;
import com.kb.wms.inbound.application.port.in.query.SupplierOrderSearchCondition;
import com.kb.wms.inbound.application.port.in.result.SupplierOrderLineView;
import com.kb.wms.inbound.application.port.in.result.SupplierOrderSummary;
import com.kb.wms.inbound.application.port.in.result.SupplierOrderView;
import com.kb.wms.inbound.application.port.out.InboundRepository;
import com.kb.wms.inbound.application.port.out.PurchaseOrderRepository;
import com.kb.wms.inbound.application.port.out.SupplierOrderQueryRepository;
import com.kb.wms.inbound.application.port.out.SupplierOrderRepository;
import com.kb.wms.inbound.application.port.out.SupplierRepository;
import com.kb.wms.inbound.domain.entity.Inbound;
import com.kb.wms.inbound.domain.entity.PurchaseOrder;
import com.kb.wms.inbound.domain.entity.PurchaseOrderLine;
import com.kb.wms.inbound.domain.entity.Supplier;
import com.kb.wms.inbound.domain.entity.SupplierOrder;
import com.kb.wms.inbound.domain.enums.InboundStatus;
import com.kb.wms.inbound.domain.enums.PurchaseOrderLineStatus;
import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;
import com.kb.wms.inbound.domain.enums.SupplierOrderStatus;
import com.kb.wms.product.adapter.out.persistence.entity.ProductSkuJpaEntity;
import com.kb.wms.product.adapter.out.persistence.repository.ProductSkuJpaRepository;
import com.kb.wms.product.domain.enums.ProductStatus;
import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseJpaEntity;
import com.kb.wms.warehouse.adapter.out.persistence.repository.WarehouseJpaRepository;
import com.kb.wms.warehouse.domain.enums.WarehouseStatus;

import jakarta.persistence.EntityManager;

/**
 * 공급처 발주서 영속성 어댑터(저장·락 조회·조회 전용 쿼리) 검증.
 *
 * <p>데이터: 창고 2개(서울/부산), 공급처 2곳, SKU 2개
 * <ul>
 *   <li>PO1(서울, CONFIRMED): SKU-A 100×1,000 → SO1(공급처1, ISSUED) / SKU-B 50×2,000 → SO2(공급처2, ISSUED)</li>
 *   <li>PO2(부산, CONFIRMED): SKU-A 10×1,000 → SO3(공급처1, COMPLETED)</li>
 * </ul>
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "spring.flyway.enabled=false")
class SupplierOrderPersistenceAdapterTest {

    private static final PageCondition PAGE = TestPages.firstPage(
            TestPages.order("so.createdAt", SortDirection.DESC),
            TestPages.order("so.supplierOrderId", SortDirection.DESC));

    @Test
    @DisplayName("페이지 크기만큼 자르고 전체 건수는 발주서 건수로 센다")
    void search_paging() {
        SupplierOrderSearchCondition all = SupplierOrderSearchCondition.unscoped(null, null, null, null, null, null, null);

        PageResult<SupplierOrderSummary> first = queryRepository.search(all, new PageCondition(1, 2, PAGE.orders()));
        PageResult<SupplierOrderSummary> second = queryRepository.search(all, new PageCondition(2, 2, PAGE.orders()));

        assertThat(first.items()).extracting(SupplierOrderSummary::supplierOrderId).containsExactly(so3, so2);
        assertThat(second.items()).extracting(SupplierOrderSummary::supplierOrderId).containsExactly(so1);
        assertThat(first.totalElements()).isEqualTo(3);
        assertThat(first.totalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("전체 건수는 필터를 적용한 발주서 건수다")
    void search_pagingCountFollowsFilter() {
        PageResult<SupplierOrderSummary> result = queryRepository.search(
                SupplierOrderSearchCondition.unscoped(null, null, supplier1, null, null, null, null),
                new PageCondition(1, 1, PAGE.orders()));

        assertThat(result.items()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("조인한 공급처명으로 정렬하고, 같은 값은 발주서 ID로 순서를 고정한다")
    void search_sortedBySupplierName() {
        PageCondition bySupplier = TestPages.firstPage(
                TestPages.order("s.name", SortDirection.DESC), TestPages.order("so.supplierOrderId", SortDirection.DESC));

        // 다라 상사(SO2) → 가나 상사(ID 내림차순: SO3, SO1)
        assertThat(queryRepository.search(
                SupplierOrderSearchCondition.unscoped(null, null, null, null, null, null, null), bySupplier).items())
                .extracting(SupplierOrderSummary::supplierOrderId).containsExactly(so2, so3, so1);
    }

    @Autowired EntityManager entityManager;
    @Autowired SupplierOrderRepository supplierOrderRepository;
    @Autowired SupplierOrderQueryRepository queryRepository;
    @Autowired PurchaseOrderRepository purchaseOrderRepository;
    @Autowired InboundRepository inboundRepository;
    @Autowired SupplierRepository supplierRepository;
    @Autowired WarehouseJpaRepository warehouseJpaRepository;
    @Autowired ProductSkuJpaRepository productSkuJpaRepository;

    Long seoul;
    Long busan;
    Long supplier1;
    Long supplier2;
    Long skuA;
    Long skuB;
    Long po1;
    Long po2;
    Long so1;
    Long so2;
    Long so3;
    Long po1LineA;
    Long po1LineB;

    @BeforeEach
    void setUp() {
        seoul = warehouse("WH-SO-SEOUL", "서울 물류센터");
        busan = warehouse("WH-SO-BUSAN", "부산 물류센터");
        supplier1 = supplier("SUP-SO-1", "가나 상사");
        supplier2 = supplier("SUP-SO-2", "다라 상사");
        skuA = sku("SKU-SO-A", "에이 상품");
        skuB = sku("SKU-SO-B", "비 상품");

        po1 = order("PO-SO-0001", seoul);
        po2 = order("PO-SO-0002", busan);
        po1LineA = purchaseOrderRepository.saveLines(List.of(
                PurchaseOrderLine.register(po1, skuA, 100, new BigDecimal("1000")))).get(0).getPurchaseOrderLineId();
        po1LineB = purchaseOrderRepository.saveLines(List.of(
                PurchaseOrderLine.register(po1, skuB, 50, new BigDecimal("2000")))).get(0).getPurchaseOrderLineId();
        Long po2Line = purchaseOrderRepository.saveLines(List.of(
                PurchaseOrderLine.register(po2, skuA, 10, new BigDecimal("1000")))).get(0).getPurchaseOrderLineId();

        so1 = issue("SO-20261001-0001", po1, supplier1, po1LineA);
        so2 = issue("SO-20261001-0002", po1, supplier2, po1LineB);
        so3 = issue("SO-20261002-0001", po2, supplier1, po2Line);
        SupplierOrder completed = supplierOrderRepository.findById(so3).orElseThrow();
        completed.complete();
        supplierOrderRepository.save(completed);
    }

    @Test
    @DisplayName("저장한 발주서를 다시 조회할 수 있고, 없는 발주서는 빈 값이다")
    void saveAndFind() {
        SupplierOrder found = supplierOrderRepository.findById(so1).orElseThrow();

        assertThat(found.getSupplierOrderNo()).isEqualTo("SO-20261001-0001");
        assertThat(found.getPurchaseOrderId()).isEqualTo(po1);
        assertThat(found.getSupplierId()).isEqualTo(supplier1);
        assertThat(found.getStatus()).isEqualTo(SupplierOrderStatus.ISSUED);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(supplierOrderRepository.findById(999_999L)).isEmpty();
        assertThat(supplierOrderRepository.findByIdForUpdate(999_999L)).isEmpty();
        assertThat(queryRepository.findView(999_999L)).isEmpty();
    }

    @Test
    @DisplayName("상태 변경을 저장하면 반영되고, 락 조회도 같은 값을 돌려준다")
    void saveStatusChange() {
        SupplierOrder order = supplierOrderRepository.findByIdForUpdate(so1).orElseThrow();
        order.markPartiallyReceived();
        supplierOrderRepository.save(order);

        assertThat(supplierOrderRepository.findById(so1).orElseThrow().getStatus())
                .isEqualTo(SupplierOrderStatus.PARTIALLY_RECEIVED);
    }

    @Test
    @DisplayName("요청에 속한 발주서를 발주서 ID 오름차순으로 조회한다")
    void findByPurchaseOrderId() {
        assertThat(supplierOrderRepository.findByPurchaseOrderId(po1))
                .extracting(SupplierOrder::getSupplierOrderId).containsExactly(so1, so2);
        assertThat(supplierOrderRepository.findByPurchaseOrderIdForUpdate(po1))
                .extracting(SupplierOrder::getSupplierOrderId).containsExactly(so1, so2);
        assertThat(supplierOrderRepository.findByPurchaseOrderId(999_999L)).isEmpty();
    }

    @Test
    @DisplayName("발주서 번호 존재 여부와 접두어 개수를 센다")
    void numberQueries() {
        assertThat(supplierOrderRepository.existsBySupplierOrderNo("SO-20261001-0001")).isTrue();
        assertThat(supplierOrderRepository.existsBySupplierOrderNo("SO-20261001-9999")).isFalse();

        assertThat(supplierOrderRepository.countBySupplierOrderNoPrefix("SO-20261001-")).isEqualTo(2);
        assertThat(supplierOrderRepository.countBySupplierOrderNoPrefix("SO-20261002-")).isEqualTo(1);
        assertThat(supplierOrderRepository.countBySupplierOrderNoPrefix("SO-20261003-")).isZero();
    }

    @Test
    @DisplayName("같은 (요청, 공급처) 쌍은 두 번 발행할 수 없다")
    void duplicatePair() {
        assertThatThrownBy(() -> {
            supplierOrderRepository.save(SupplierOrder.issue("SO-20261001-0099", po1, supplier1));
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("공급처의 진행 중 발주서(ISSUED·PARTIALLY_RECEIVED)가 있는지 확인한다")
    void existsInProgressBySupplierId() {
        assertThat(supplierOrderRepository.existsInProgressBySupplierId(supplier1)).isTrue();  // SO1 ISSUED
        assertThat(supplierOrderRepository.existsInProgressBySupplierId(supplier2)).isTrue();  // SO2 ISSUED

        SupplierOrder so1Order = supplierOrderRepository.findById(so1).orElseThrow();
        so1Order.complete();
        supplierOrderRepository.save(so1Order);
        assertThat(supplierOrderRepository.existsInProgressBySupplierId(supplier1)).isFalse(); // SO1·SO3 COMPLETED

        SupplierOrder so2Order = supplierOrderRepository.findById(so2).orElseThrow();
        so2Order.cancel();
        supplierOrderRepository.save(so2Order);
        assertThat(supplierOrderRepository.existsInProgressBySupplierId(supplier2)).isFalse(); // CANCELED
    }

    @Test
    @DisplayName("라인의 발주서 연결이 저장되고 발주서 기준으로 조회·락 조회된다")
    void linesBySupplierOrder() {
        assertThat(purchaseOrderRepository.findLinesBySupplierOrderId(so1))
                .extracting(PurchaseOrderLine::getPurchaseOrderLineId).containsExactly(po1LineA);
        assertThat(purchaseOrderRepository.findLinesBySupplierOrderIdForUpdate(so2))
                .extracting(PurchaseOrderLine::getPurchaseOrderLineId).containsExactly(po1LineB);
        assertThat(purchaseOrderRepository.findLineById(po1LineA).orElseThrow().getSupplierOrderId()).isEqualTo(so1);
        assertThat(purchaseOrderRepository.findLinesBySupplierOrderId(999_999L)).isEmpty();
    }

    @Test
    @DisplayName("발주서에 연결된 입고 중 진행 중(ARRIVED·INSPECTING)인 것만 진행 중으로 본다")
    void inboundInProgressBySupplierOrder() {
        assertThat(inboundRepository.existsInProgressBySupplierOrderId(so1)).isFalse();

        Long inboundId = inboundRepository.save(inbound("IB-SO-0001", po1, so1, InboundStatus.ARRIVED)).getInboundId();
        assertThat(inboundRepository.existsInProgressBySupplierOrderId(so1)).isTrue();
        assertThat(inboundRepository.existsInProgressBySupplierOrderId(so2)).isFalse();
        assertThat(inboundRepository.findById(inboundId).orElseThrow().getSupplierOrderId()).isEqualTo(so1);

        Inbound completed = inboundRepository.findById(inboundId).orElseThrow();
        completed.inspect();
        completed.complete(1L, LocalDateTime.now());
        inboundRepository.save(completed);
        assertThat(inboundRepository.existsInProgressBySupplierOrderId(so1)).isFalse();
    }

    @Test
    @DisplayName("목록은 등록 일시·ID 내림차순이며 공급처명·창고명·항목 수·금액 합계를 담는다")
    void search_all() {
        List<SupplierOrderSummary> all = queryRepository.search(
                SupplierOrderSearchCondition.unscoped(null, null, null, null, null, null, null), PAGE).items();

        assertThat(all).extracting(SupplierOrderSummary::supplierOrderId).containsExactly(so3, so2, so1);

        SupplierOrderSummary s1 = byId(all, so1);
        assertThat(s1.supplierOrderNo()).isEqualTo("SO-20261001-0001");
        assertThat(s1.purchaseOrderId()).isEqualTo(po1);
        assertThat(s1.purchaseOrderNo()).isEqualTo("PO-SO-0001");
        assertThat(s1.supplierName()).isEqualTo("가나 상사");
        assertThat(s1.warehouseName()).isEqualTo("서울 물류센터");
        assertThat(s1.status()).isEqualTo(SupplierOrderStatus.ISSUED);
        assertThat(s1.lineCount()).isEqualTo(1L);
        assertThat(s1.totalAmount()).isEqualByComparingTo("100000");
        assertThat(byId(all, so2).totalAmount()).isEqualByComparingTo("100000");
    }

    @Test
    @DisplayName("상태·창고·공급처·요청·키워드 조건으로 거른다")
    void search_filters() {
        assertThat(search(SupplierOrderStatus.COMPLETED, null, null, null, null))
                .extracting(SupplierOrderSummary::supplierOrderId).containsExactly(so3);
        assertThat(search(null, busan, null, null, null))
                .extracting(SupplierOrderSummary::supplierOrderId).containsExactly(so3);
        assertThat(search(null, null, supplier1, null, null))
                .extracting(SupplierOrderSummary::supplierOrderId).containsExactly(so3, so1);
        assertThat(search(null, null, null, po1, null))
                .extracting(SupplierOrderSummary::supplierOrderId).containsExactly(so2, so1);
        assertThat(search(null, null, null, null, "20261002"))
                .extracting(SupplierOrderSummary::supplierOrderId).containsExactly(so3);
        assertThat(search(null, null, null, null, "po-so-0002"))
                .extracting(SupplierOrderSummary::supplierOrderId).containsExactly(so3);
        assertThat(search(null, null, null, null, "없는키워드")).isEmpty();
    }

    @Test
    @DisplayName("담당 창고 범위를 주면 그 창고의 발주서만, 빈 범위면 아무것도 보이지 않는다")
    void search_scoped() {
        List<SupplierOrderSummary> seoulOnly = queryRepository.search(new SupplierOrderSearchCondition(
                null, null, null, null, null, null, null, List.of(seoul)), PAGE).items();
        assertThat(seoulOnly).extracting(SupplierOrderSummary::supplierOrderId).containsExactly(so2, so1);

        List<SupplierOrderSummary> none = queryRepository.search(new SupplierOrderSearchCondition(
                null, null, null, null, null, null, null, List.of()), PAGE).items();
        assertThat(none).isEmpty();
    }

    @Test
    @DisplayName("요청에 속한 발주서 요약을 발주서 ID 오름차순으로 돌려준다")
    void findSummariesByPurchaseOrderId() {
        assertThat(queryRepository.findSummariesByPurchaseOrderId(po1))
                .extracting(SupplierOrderSummary::supplierOrderId).containsExactly(so1, so2);
        assertThat(queryRepository.findSummariesByPurchaseOrderId(999_999L)).isEmpty();
    }

    @Test
    @DisplayName("상세 헤더와 항목 뷰를 돌려준다")
    void viewAndLineViews() {
        SupplierOrderView view = queryRepository.findView(so1).orElseThrow();
        assertThat(view.supplierOrderNo()).isEqualTo("SO-20261001-0001");
        assertThat(view.supplierName()).isEqualTo("가나 상사");
        assertThat(view.warehouseId()).isEqualTo(seoul);
        assertThat(view.lineCount()).isEqualTo(1L);
        assertThat(view.totalAmount()).isEqualByComparingTo("100000");
        assertThat(view.updatedAt()).isNotNull();

        List<SupplierOrderLineView> lines = queryRepository.findLineViews(so1);
        assertThat(lines).hasSize(1);
        SupplierOrderLineView line = lines.get(0);
        assertThat(line.purchaseOrderLineId()).isEqualTo(po1LineA);
        assertThat(line.skuId()).isEqualTo(skuA);
        assertThat(line.skuCode()).isEqualTo("SKU-SO-A");
        assertThat(line.unit()).isEqualTo("EA");
        assertThat(line.expectedQuantity()).isEqualTo(100L);
        assertThat(line.receivedQuantity()).isZero();
        assertThat(line.remainingQuantity()).isEqualTo(100L);
        assertThat(line.status()).isEqualTo(PurchaseOrderLineStatus.REQUESTED);
        assertThat(queryRepository.findLineViews(999_999L)).isEmpty();
    }

    private List<SupplierOrderSummary> search(SupplierOrderStatus status, Long warehouseId, Long supplierId,
                                              Long purchaseOrderId, String keyword) {
        return queryRepository.search(SupplierOrderSearchCondition.unscoped(
                status, warehouseId, supplierId, purchaseOrderId, keyword, null, null), PAGE).items();
    }

    private SupplierOrderSummary byId(List<SupplierOrderSummary> list, Long id) {
        return list.stream().filter(s -> s.supplierOrderId().equals(id)).findFirst().orElseThrow();
    }

    private Long warehouse(String code, String name) {
        return warehouseJpaRepository.save(WarehouseJpaEntity.builder()
                .warehouseCode(code).name(name).address("주소").totalCapacity(BigDecimal.valueOf(1000))
                .status(WarehouseStatus.ACTIVE)
                .build()).getWarehouseId();
    }

    private Long supplier(String code, String name) {
        return supplierRepository.save(
                Supplier.register(code, name, "담당자", "02-1234-5678", "a@b.com", "주소")).getSupplierId();
    }

    private Long sku(String code, String name) {
        return productSkuJpaRepository.save(ProductSkuJpaEntity.builder()
                .productId(1L).skuCode(code).name(name).unit("EA")
                .currentPurchasePrice(BigDecimal.ZERO).currentSupplyPrice(BigDecimal.ZERO)
                .safetyStockQuantity(0L).status(ProductStatus.ACTIVE)
                .build()).getSkuId();
    }

    private Long order(String no, Long warehouseId) {
        return purchaseOrderRepository.save(PurchaseOrder.builder()
                .purchaseOrderNo(no).warehouseId(warehouseId)
                .status(PurchaseOrderStatus.CONFIRMED).note("비고").createdBy(1L)
                .build()).getPurchaseOrderId();
    }

    /** 발주서를 발행하고 라인에 공급처 배정과 발주서 연결을 저장한다. */
    private Long issue(String no, Long purchaseOrderId, Long supplierId, Long lineId) {
        Long id = supplierOrderRepository.save(SupplierOrder.issue(no, purchaseOrderId, supplierId))
                .getSupplierOrderId();
        PurchaseOrderLine line = purchaseOrderRepository.findLineById(lineId).orElseThrow();
        line.assignSupplier(supplierId);
        line.attachToSupplierOrder(id);
        purchaseOrderRepository.saveLines(List.of(line));
        return id;
    }

    private Inbound inbound(String no, Long purchaseOrderId, Long supplierOrderId, InboundStatus status) {
        return Inbound.builder()
                .inboundNo(no).purchaseOrderId(purchaseOrderId).supplierOrderId(supplierOrderId)
                .warehouseId(seoul).status(status).arrivedAt(LocalDateTime.now())
                .build();
    }
}
