package com.kb.wms.inbound.adapter.out.persistence;

import com.kb.wms.common.paging.TestPages;
import com.kb.wms.common.paging.PageResult;
import com.kb.wms.common.paging.SortDirection;
import com.kb.wms.common.paging.PageCondition;
import com.kb.wms.auth.application.port.out.UserRepository;
import com.kb.wms.auth.domain.entity.User;
import com.kb.wms.common.security.UserRole;
import jakarta.persistence.EntityManager;

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

import com.kb.wms.inbound.application.port.in.query.PurchaseOrderSearchCondition;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderLineView;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderSummary;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderView;
import com.kb.wms.inbound.application.port.out.PurchaseOrderQueryRepository;
import com.kb.wms.inbound.application.port.out.PurchaseOrderRepository;
import com.kb.wms.inbound.application.port.out.SupplierRepository;
import com.kb.wms.inbound.domain.entity.PurchaseOrder;
import com.kb.wms.inbound.domain.entity.PurchaseOrderLine;
import com.kb.wms.inbound.domain.entity.Supplier;
import com.kb.wms.inbound.domain.enums.PurchaseOrderLineStatus;
import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;
import com.kb.wms.product.adapter.out.persistence.entity.ProductSkuJpaEntity;
import com.kb.wms.product.adapter.out.persistence.repository.ProductSkuJpaRepository;
import com.kb.wms.product.domain.enums.ProductStatus;
import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseJpaEntity;
import com.kb.wms.warehouse.adapter.out.persistence.repository.WarehouseJpaRepository;
import com.kb.wms.warehouse.domain.enums.WarehouseStatus;

/**
 * 발주 영속성 어댑터(저장·조회 전용 쿼리) 검증.
 *
 * <p>데이터: 창고 2개(서울/부산), 공급처 2곳(항목에 배정), SKU 2개(SKU-A, SKU-B)
 * <ul>
 *   <li>PO1: 서울 / 공급처1 / REQUESTED / SKU-A 100×1,000 + SKU-B 50×2,000</li>
 *   <li>PO2: 부산 / 공급처1 / CONFIRMED / SKU-A 10×1,000</li>
 *   <li>PO3: 서울 / 공급처2 / CANCELED / SKU-B 5×2,000</li>
 * </ul>
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "spring.flyway.enabled=false")
class PurchaseOrderPersistenceAdapterTest {

    private static final PageCondition PAGE = TestPages.firstPage(
            TestPages.order("po.createdAt", SortDirection.DESC),
            TestPages.order("po.purchaseOrderId", SortDirection.DESC));

    @Test
    @DisplayName("페이지 크기만큼 자르고 전체 건수는 발주 건수로 센다 (항목이 여러 개여도 1건)")
    void search_paging() {
        PageCondition byNo = TestPages.firstPage(
                TestPages.order("po.purchaseOrderNo", SortDirection.ASC),
                TestPages.order("po.purchaseOrderId", SortDirection.ASC));
        PurchaseOrderSearchCondition all = condition(null, null, null, null, null, null);

        PageResult<PurchaseOrderSummary> first = queryRepository.search(all, new PageCondition(1, 2, byNo.orders()));
        PageResult<PurchaseOrderSummary> second = queryRepository.search(all, new PageCondition(2, 2, byNo.orders()));

        assertThat(first.items()).extracting(PurchaseOrderSummary::purchaseOrderId).containsExactly(po1, po2);
        assertThat(second.items()).extracting(PurchaseOrderSummary::purchaseOrderId).containsExactly(po3);
        // 항목 합계는 4행이지만 발주는 3건
        assertThat(first.totalElements()).isEqualTo(3);
        assertThat(first.totalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("전체 건수는 공급처 필터를 적용한 발주 건수다")
    void search_pagingCountFollowsSupplierFilter() {
        PageResult<PurchaseOrderSummary> result = queryRepository.search(
                condition(null, null, supplier1, null, null, null), new PageCondition(1, 1, PAGE.orders()));

        assertThat(result.items()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("조인한 창고명으로 정렬하고, 같은 값은 발주 ID로 순서를 고정한다")
    void search_sortedByWarehouseName() {
        PageCondition byWarehouse = TestPages.firstPage(
                TestPages.order("w.name", SortDirection.DESC), TestPages.order("po.purchaseOrderId", SortDirection.DESC));

        // 서울(ID 내림차순) → 부산
        assertThat(queryRepository.search(condition(null, null, null, null, null, null), byWarehouse).items())
                .extracting(PurchaseOrderSummary::purchaseOrderId).containsExactly(po3, po1, po2);
    }

    @Autowired UserRepository userRepository;
    @Autowired EntityManager entityManager;
    @Autowired PurchaseOrderRepository purchaseOrderRepository;
    @Autowired PurchaseOrderQueryRepository queryRepository;
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
    Long po3;

    @BeforeEach
    void setUp() {
        seoul = warehouse("WH-SEOUL", "서울 물류센터");
        busan = warehouse("WH-BUSAN", "부산 물류센터");
        supplier1 = supplier("SUP-001", "가나 상사");
        supplier2 = supplier("SUP-002", "다라 상사");
        skuA = sku("SKU-A", "에이 상품");
        skuB = sku("SKU-B", "비 상품");

        po1 = order("PO-20261001-0001", seoul, supplier1, PurchaseOrderStatus.REQUESTED,
                line(skuA, 100, "1000"), line(skuB, 50, "2000"));
        po2 = order("PO-20261001-0002", busan, supplier1, PurchaseOrderStatus.CONFIRMED,
                line(skuA, 10, "1000"));
        po3 = order("PO-20261002-0001", seoul, supplier2, PurchaseOrderStatus.CANCELED,
                line(skuB, 5, "2000"));
    }

    @Test
    @DisplayName("저장한 발주 헤더와 항목을 다시 조회할 수 있다")
    void saveAndFind() {
        PurchaseOrder found = purchaseOrderRepository.findById(po1).orElseThrow();

        assertThat(found.getPurchaseOrderNo()).isEqualTo("PO-20261001-0001");
        assertThat(found.getWarehouseId()).isEqualTo(seoul);
        assertThat(found.getStatus()).isEqualTo(PurchaseOrderStatus.REQUESTED);

        List<PurchaseOrderLine> lines = purchaseOrderRepository.findLinesByPurchaseOrderId(po1);
        assertThat(lines).hasSize(2);
        assertThat(lines).extracting(PurchaseOrderLine::getSkuId).containsExactlyInAnyOrder(skuA, skuB);
        assertThat(lines).allSatisfy(l -> {
            assertThat(l.getReceivedQuantity()).isZero();
            assertThat(l.getSupplierId()).isEqualTo(supplier1);
            assertThat(l.getSupplierOrderId()).isNull();
            assertThat(l.getStatus()).isEqualTo(PurchaseOrderLineStatus.REQUESTED);
        });
    }

    @Test
    @DisplayName("없는 발주는 빈 값을 돌려준다")
    void findById_notFound() {
        assertThat(purchaseOrderRepository.findById(999_999L)).isEmpty();
        assertThat(queryRepository.findView(999_999L)).isEmpty();
    }

    @Test
    @DisplayName("상태 변경을 저장하면 반영된다")
    void saveStatusChange() {
        PurchaseOrder po = purchaseOrderRepository.findById(po1).orElseThrow();
        po.confirm();
        purchaseOrderRepository.save(po);

        assertThat(purchaseOrderRepository.findById(po1).orElseThrow().getStatus())
                .isEqualTo(PurchaseOrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("저장 결과에는 감사 필드(updatedAt)가 채워져 있다 (상태 응답이 이 값을 쓴다)")
    void save_returnsUpdatedAt() {
        PurchaseOrder po = purchaseOrderRepository.findById(po1).orElseThrow();
        po.confirm();

        PurchaseOrder saved = purchaseOrderRepository.save(po);

        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("발주 번호 존재 여부와 접두어 개수를 센다")
    void numberQueries() {
        assertThat(purchaseOrderRepository.existsByPurchaseOrderNo("PO-20261001-0001")).isTrue();
        assertThat(purchaseOrderRepository.existsByPurchaseOrderNo("PO-20261001-9999")).isFalse();

        assertThat(purchaseOrderRepository.countByPurchaseOrderNoPrefix("PO-20261001-")).isEqualTo(2);
        assertThat(purchaseOrderRepository.countByPurchaseOrderNoPrefix("PO-20261002-")).isEqualTo(1);
        assertThat(purchaseOrderRepository.countByPurchaseOrderNoPrefix("PO-20261003-")).isZero();
    }

    @Test
    @DisplayName("같은 발주 번호는 저장할 수 없다 (uk_purchase_order_no)")
    void duplicatePurchaseOrderNo() {
        assertThatThrownBy(() -> purchaseOrderRepository.save(
                PurchaseOrder.register("PO-20261001-0001", seoul, null, null, 1L)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("한 발주에 같은 SKU 항목을 두 번 저장할 수 없다")
    void duplicateSkuInOrder() {
        assertThatThrownBy(() -> purchaseOrderRepository.saveLines(List.of(line(po1, skuA, 1, "1000"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------- search ----------

    @Test
    @DisplayName("조건이 없으면 전체 발주를 항목 수·금액 집계와 함께 조회한다")
    void search_all() {
        List<PurchaseOrderSummary> result = queryRepository.search(condition(null, null, null, null, null, null), PAGE).items();

        assertThat(result).extracting(PurchaseOrderSummary::purchaseOrderId)
                .containsExactlyInAnyOrder(po1, po2, po3);

        PurchaseOrderSummary s1 = byId(result, po1);
        assertThat(s1.purchaseOrderNo()).isEqualTo("PO-20261001-0001");
        assertThat(s1.warehouseName()).isEqualTo("서울 물류센터");
        assertThat(s1.suppliers()).isEmpty();
        assertThat(s1.status()).isEqualTo(PurchaseOrderStatus.REQUESTED);
        assertThat(s1.lineCount()).isEqualTo(2L);
        // 100×1000 + 50×2000
        assertThat(s1.totalAmount()).isEqualByComparingTo("200000");

        PurchaseOrderSummary s2 = byId(result, po2);
        assertThat(s2.lineCount()).isEqualTo(1L);
        assertThat(s2.totalAmount()).isEqualByComparingTo("10000");
        assertThat(s2.warehouseName()).isEqualTo("부산 물류센터");
    }

    @Test
    @DisplayName("최신 등록 순(번호 내림차순)으로 정렬된다")
    void search_orderedByLatest() {
        List<PurchaseOrderSummary> result = queryRepository.search(condition(null, null, null, null, null, null), PAGE).items();

        assertThat(result).extracting(PurchaseOrderSummary::purchaseOrderId)
                .isSortedAccordingTo(java.util.Comparator.reverseOrder());
    }

    @Test
    @DisplayName("상태로 필터링한다")
    void search_byStatus() {
        assertThat(queryRepository.search(condition(PurchaseOrderStatus.CONFIRMED, null, null, null, null, null), PAGE).items())
                .extracting(PurchaseOrderSummary::purchaseOrderId).containsExactly(po2);
        assertThat(queryRepository.search(condition(PurchaseOrderStatus.COMPLETED, null, null, null, null, null), PAGE).items())
                .isEmpty();
    }

    @Test
    @DisplayName("창고 범위(담당 창고 목록)로 좁히고, 빈 목록이면 비어 있다")
    void search_byWarehouseScope() {
        assertThat(queryRepository.search(new PurchaseOrderSearchCondition(
                null, null, null, null, null, null, List.of(seoul)), PAGE).items())
                .extracting(PurchaseOrderSummary::purchaseOrderId).containsExactlyInAnyOrder(po1, po3);
        assertThat(queryRepository.search(new PurchaseOrderSearchCondition(
                null, null, null, null, null, null, List.of(seoul, busan)), PAGE).items()).hasSize(
                queryRepository.search(condition(null, null, null, null, null, null), PAGE).items().size());
        assertThat(queryRepository.search(new PurchaseOrderSearchCondition(
                null, null, null, null, null, null, List.of()), PAGE).items()).isEmpty();
    }

    @Test
    @DisplayName("창고·공급처로 필터링한다")
    void search_byWarehouseAndSupplier() {
        assertThat(queryRepository.search(condition(null, seoul, null, null, null, null), PAGE).items())
                .extracting(PurchaseOrderSummary::purchaseOrderId).containsExactlyInAnyOrder(po1, po3);
        assertThat(queryRepository.search(condition(null, null, supplier1, null, null, null), PAGE).items())
                .extracting(PurchaseOrderSummary::purchaseOrderId).containsExactlyInAnyOrder(po1, po2);
        assertThat(queryRepository.search(condition(null, seoul, supplier1, null, null, null), PAGE).items())
                .extracting(PurchaseOrderSummary::purchaseOrderId).containsExactly(po1);
    }

    @Test
    @DisplayName("키워드는 발주 번호를 대소문자 구분 없이 부분 일치로 찾는다")
    void search_byKeyword() {
        assertThat(queryRepository.search(condition(null, null, null, "20261002", null, null), PAGE).items())
                .extracting(PurchaseOrderSummary::purchaseOrderId).containsExactly(po3);
        assertThat(queryRepository.search(condition(null, null, null, "po-20261001", null, null), PAGE).items())
                .extracting(PurchaseOrderSummary::purchaseOrderId).containsExactlyInAnyOrder(po1, po2);
        assertThat(queryRepository.search(condition(null, null, null, "NOPE", null, null), PAGE).items()).isEmpty();
    }

    @Test
    @DisplayName("등록일 범위로 필터링한다")
    void search_byCreatedRange() {
        LocalDateTime now = LocalDateTime.now();

        assertThat(queryRepository.search(
                condition(null, null, null, null, now.minusDays(1), now.plusDays(1)), PAGE).items()).hasSize(3);
        assertThat(queryRepository.search(
                condition(null, null, null, null, now.plusDays(1), null), PAGE).items()).isEmpty();
        assertThat(queryRepository.search(
                condition(null, null, null, null, null, now.minusDays(1)), PAGE).items()).isEmpty();
    }

    @Test
    @DisplayName("항목이 없는 발주는 목록에 나오지 않는다")
    void search_excludesOrderWithoutLines() {
        Long empty = purchaseOrderRepository.save(
                PurchaseOrder.register("PO-20261002-0099", seoul, null, null, 1L)).getPurchaseOrderId();

        assertThat(queryRepository.search(condition(null, null, null, null, null, null), PAGE).items())
                .extracting(PurchaseOrderSummary::purchaseOrderId).doesNotContain(empty);
    }

    // ---------- view ----------

    @Test
    @DisplayName("발주 상세 헤더를 집계 값과 함께 조회한다")
    void findView() {
        PurchaseOrderView view = queryRepository.findView(po1).orElseThrow();

        assertThat(view.purchaseOrderNo()).isEqualTo("PO-20261001-0001");
        assertThat(view.warehouseName()).isEqualTo("서울 물류센터");
        assertThat(view.supplierOrders()).isEmpty();
        assertThat(view.status()).isEqualTo(PurchaseOrderStatus.REQUESTED);
        assertThat(view.note()).isEqualTo("비고");
        assertThat(view.lineCount()).isEqualTo(2L);
        assertThat(view.totalAmount()).isEqualByComparingTo("200000");
        assertThat(view.createdBy()).isEqualTo(1L);
    }

    @Test
    @DisplayName("발주 항목 뷰는 SKU 코드순으로 나오고 잔여 수량을 계산한다")
    void findLineViews() {
        List<PurchaseOrderLineView> lines = queryRepository.findLineViews(po1);

        assertThat(lines).extracting(PurchaseOrderLineView::skuCode).containsExactly("SKU-A", "SKU-B");

        PurchaseOrderLineView a = lines.get(0);
        assertThat(a.skuName()).isEqualTo("에이 상품");
        assertThat(a.unit()).isEqualTo("EA");
        assertThat(a.expectedQuantity()).isEqualTo(100L);
        assertThat(a.receivedQuantity()).isZero();
        assertThat(a.remainingQuantity()).isEqualTo(100L);
        assertThat(a.orderedUnitPrice()).isEqualByComparingTo("1000");
        assertThat(a.lineAmount()).isEqualByComparingTo("100000");
        assertThat(a.status()).isEqualTo(PurchaseOrderLineStatus.REQUESTED);
    }

    @Test
    @DisplayName("부분 입고·초과 입고 항목의 잔여 수량은 0 미만으로 내려가지 않는다")
    void findLineViews_remainingQuantity() {
        List<PurchaseOrderLine> lines = purchaseOrderRepository.findLinesByPurchaseOrderId(po1);
        PurchaseOrderLine a = lines.stream().filter(l -> l.getSkuId().equals(skuA)).findFirst().orElseThrow();
        PurchaseOrderLine b = lines.stream().filter(l -> l.getSkuId().equals(skuB)).findFirst().orElseThrow();
        a.receive(30);   // 100 중 30 입고
        b.receive(60);   // 50 중 60 입고(초과)
        purchaseOrderRepository.saveLines(List.of(a, b));

        List<PurchaseOrderLineView> views = queryRepository.findLineViews(po1);

        PurchaseOrderLineView viewA = views.get(0);
        assertThat(viewA.receivedQuantity()).isEqualTo(30L);
        assertThat(viewA.remainingQuantity()).isEqualTo(70L);
        assertThat(viewA.status()).isEqualTo(PurchaseOrderLineStatus.PARTIALLY_RECEIVED);

        PurchaseOrderLineView viewB = views.get(1);
        assertThat(viewB.receivedQuantity()).isEqualTo(60L);
        assertThat(viewB.remainingQuantity()).isZero();
        assertThat(viewB.status()).isEqualTo(PurchaseOrderLineStatus.COMPLETED);
    }

    // ---------- line supplier ----------

    @Test
    @DisplayName("라인에 배정한 공급처를 바꿔 저장하고, 항목 뷰에 공급처명과 발주서 ID를 담는다")
    void lineSupplier_roundTrip() {
        List<PurchaseOrderLine> lines = purchaseOrderRepository.findLinesByPurchaseOrderId(po1);
        PurchaseOrderLine a = lines.stream().filter(l -> l.getSkuId().equals(skuA)).findFirst().orElseThrow();
        a.assignSupplier(supplier2);
        purchaseOrderRepository.saveLines(List.of(a));
        entityManager.flush();
        entityManager.clear();

        List<PurchaseOrderLineView> views = queryRepository.findLineViews(po1);

        PurchaseOrderLineView viewA = views.get(0);
        assertThat(viewA.skuCode()).isEqualTo("SKU-A");
        assertThat(viewA.supplierId()).isEqualTo(supplier2);
        assertThat(viewA.supplierName()).isEqualTo("다라 상사");
        assertThat(viewA.supplierOrderId()).isNull();
        PurchaseOrderLineView viewB = views.get(1);
        assertThat(viewB.supplierId()).isEqualTo(supplier1);
        assertThat(viewB.supplierName()).isEqualTo("가나 상사");
    }

    @Test
    @DisplayName("공급처가 배정되기 전 라인은 공급처가 null이고, 공급처 필터와 공급처 목록에서 빠진다")
    void lineWithoutSupplier() {
        Long id = purchaseOrderRepository.save(PurchaseOrder.builder()
                .purchaseOrderNo("PO-20261003-0001").warehouseId(seoul)
                .status(PurchaseOrderStatus.REQUESTED).createdBy(1L).build()).getPurchaseOrderId();
        purchaseOrderRepository.saveLines(List.of(line(id, skuA, 10, "1000")));

        PurchaseOrderLineView view = queryRepository.findLineViews(id).get(0);
        assertThat(view.supplierId()).isNull();
        assertThat(view.supplierName()).isNull();
        assertThat(queryRepository.findView(id)).isPresent();
        assertThat(queryRepository.search(condition(null, null, supplier1, null, null, null), PAGE).items())
                .extracting(PurchaseOrderSummary::purchaseOrderId).doesNotContain(id);
        assertThat(queryRepository.findSuppliers(List.of(id))).isEmpty();
    }

    @Test
    @DisplayName("발주별 배정 공급처는 중복 없이 발주·공급처 ID 순으로 나오고, 빈 목록이면 빈 결과다")
    void findSuppliers() {
        Long mixed = order("PO-20261003-0002", seoul, null, PurchaseOrderStatus.CONFIRMED,
                line(skuA, 10, "1000"), line(skuB, 5, "2000"));
        List<PurchaseOrderLine> lines = purchaseOrderRepository.findLinesByPurchaseOrderId(mixed);
        lines.get(0).assignSupplier(supplier2);
        lines.get(1).assignSupplier(supplier2);
        purchaseOrderRepository.saveLines(lines);

        assertThat(queryRepository.findSuppliers(List.of(po1, mixed, po3)))
                .extracting(s -> s.purchaseOrderId() + "/" + s.supplierId() + "/" + s.supplierName())
                .containsExactly(
                        po1 + "/" + supplier1 + "/가나 상사",
                        po3 + "/" + supplier2 + "/다라 상사",
                        mixed + "/" + supplier2 + "/다라 상사");
        assertThat(queryRepository.findSuppliers(List.of())).isEmpty();
    }

    // ---------- fixtures ----------

    private PurchaseOrderSearchCondition condition(PurchaseOrderStatus status, Long warehouseId, Long supplierId,
                                                   String keyword, LocalDateTime from, LocalDateTime to) {
        return PurchaseOrderSearchCondition.unscoped(status, warehouseId, supplierId, keyword, from, to);
    }

    private PurchaseOrderSummary byId(List<PurchaseOrderSummary> list, Long id) {
        return list.stream().filter(s -> s.purchaseOrderId().equals(id)).findFirst().orElseThrow();
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
                .productId(1L).skuCode(code).name(name).unit("EA").currentPurchasePrice(java.math.BigDecimal.ZERO).currentSupplyPrice(java.math.BigDecimal.ZERO)
                .safetyStockQuantity(0L).status(ProductStatus.ACTIVE)
                .build()).getSkuId();
    }

    /** 아직 발주 ID가 없는 항목(주문 저장 시 ID를 채운다). */
    private PurchaseOrderLine line(Long skuId, long quantity, String price) {
        return PurchaseOrderLine.register(null, skuId, quantity, new BigDecimal(price));
    }

    private PurchaseOrderLine line(Long purchaseOrderId, Long skuId, long quantity, String price) {
        return PurchaseOrderLine.register(purchaseOrderId, skuId, quantity, new BigDecimal(price));
    }

    /** supplierId가 있으면 모든 항목에 그 공급처를 배정한다(공급처는 헤더가 아니라 항목에 있다). */
    private Long order(String no, Long warehouseId, Long supplierId, PurchaseOrderStatus status,
                       PurchaseOrderLine... lines) {
        PurchaseOrder po = PurchaseOrder.builder()
                .purchaseOrderNo(no).warehouseId(warehouseId)
                .status(status).note("비고").createdBy(1L)
                .build();
        Long id = purchaseOrderRepository.save(po).getPurchaseOrderId();
        List<PurchaseOrderLine> withId = java.util.Arrays.stream(lines)
                .map(l -> {
                    PurchaseOrderLine copy = line(
                            id, l.getSkuId(), l.getExpectedQuantity(), l.getOrderedUnitPrice().toPlainString());
                    if (supplierId != null) {
                        copy.assignSupplier(supplierId);
                    }
                    return copy;
                })
                .toList();
        purchaseOrderRepository.saveLines(withId);
        return id;
    }

    @Test
    @DisplayName("작성자 이름을 사용자 테이블 조인으로 상세·목록에 담고, 사용자가 없으면 이름만 null이다")
    void createdByName() {
        User writer = userRepository.save(User.signUp("po_writer", "hashed", "김작성", "po_writer@example.com",
                "010-1234-5678", UserRole.WAREHOUSE_MANAGER));
        entityManager.createQuery(
                        "update PurchaseOrderJpaEntity po set po.createdBy = :userId where po.purchaseOrderId = :id")
                .setParameter("userId", writer.getUserId()).setParameter("id", po1).executeUpdate();
        // 방금 만든 사용자와 절대 겹치지 않는 존재하지 않는 사용자 ID
        long nobody = writer.getUserId() + 1_000_000L;
        entityManager.createQuery(
                        "update PurchaseOrderJpaEntity po set po.createdBy = :userId where po.purchaseOrderId = :id")
                .setParameter("userId", nobody).setParameter("id", po2).executeUpdate();
        entityManager.clear();

        assertThat(queryRepository.findView(po1).orElseThrow().createdByName()).isEqualTo("김작성");
        List<PurchaseOrderSummary> result = queryRepository.search(condition(null, null, null, null, null, null), PAGE).items();
        assertThat(byId(result, po1).createdByName()).isEqualTo("김작성");
        assertThat(byId(result, po2).createdBy()).isEqualTo(nobody);
        assertThat(byId(result, po2).createdByName()).isNull();
        assertThat(queryRepository.findView(po2).orElseThrow().createdByName()).isNull();
    }
}
