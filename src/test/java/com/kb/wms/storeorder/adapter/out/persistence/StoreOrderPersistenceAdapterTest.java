package com.kb.wms.storeorder.adapter.out.persistence;

import com.kb.wms.auth.application.port.out.UserRepository;
import com.kb.wms.auth.domain.entity.User;
import com.kb.wms.auth.domain.enums.UserRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import com.kb.wms.product.adapter.out.persistence.entity.ProductSkuJpaEntity;
import com.kb.wms.product.adapter.out.persistence.repository.ProductSkuJpaRepository;
import com.kb.wms.product.domain.enums.ProductStatus;
import com.kb.wms.store.adapter.out.persistence.entity.StoreJpaEntity;
import com.kb.wms.store.adapter.out.persistence.repository.StoreJpaRepository;
import com.kb.wms.store.domain.enums.StoreStatus;
import com.kb.wms.storeorder.application.port.in.query.StoreOrderSearchCondition;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderLineView;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderSummary;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderView;
import com.kb.wms.storeorder.application.port.out.StoreOrderQueryRepository;
import com.kb.wms.storeorder.application.port.out.StoreOrderRepository;
import com.kb.wms.storeorder.domain.entity.StoreOrder;
import com.kb.wms.storeorder.domain.entity.StoreOrderLine;
import com.kb.wms.storeorder.domain.enums.StoreOrderLineStatus;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;
import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseJpaEntity;
import com.kb.wms.warehouse.adapter.out.persistence.repository.WarehouseJpaRepository;
import com.kb.wms.warehouse.domain.enums.WarehouseStatus;

/**
 * 지점 발주 영속성 어댑터(저장·락·채번·조회 전용 쿼리) 검증.
 *
 * <p>데이터: 지점 2개(강남/부산), 창고 2개(서울/부산), SKU 2개(SKU-A, SKU-B)
 * <ul>
 *   <li>SO1: 강남 / 미배정 / REQUESTED / 10-01 / SKU-A 100×1,000 + SKU-B 50×2,000 (출고 0)</li>
 *   <li>SO2: 강남 / 서울 / ASSIGNED / 10-02 / SKU-A 10×1,000 (4개 출고, 부족 있음)</li>
 *   <li>SO3: 부산 / 부산 / COMPLETED / 10-03 / SKU-B 5×2,000 (전량 출고)</li>
 * </ul>
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "spring.flyway.enabled=false")
class StoreOrderPersistenceAdapterTest {

    @Autowired UserRepository userRepository;
    @Autowired StoreOrderRepository storeOrderRepository;
    @Autowired StoreOrderQueryRepository queryRepository;
    @Autowired StoreJpaRepository storeJpaRepository;
    @Autowired WarehouseJpaRepository warehouseJpaRepository;
    @Autowired ProductSkuJpaRepository productSkuJpaRepository;
    @Autowired EntityManager entityManager;

    Long gangnam;
    Long busanStore;
    Long seoul;
    Long busan;
    Long skuA;
    Long skuB;
    Long so1;
    Long so2;
    Long so3;

    @BeforeEach
    void setUp() {
        gangnam = store("ST-GANGNAM", "강남점");
        busanStore = store("ST-BUSAN", "부산점");
        seoul = warehouse("WH-SEOUL", "서울 물류센터");
        busan = warehouse("WH-BUSAN", "부산 물류센터");
        skuA = sku("SKU-A", "에이 상품");
        skuB = sku("SKU-B", "비 상품");

        so1 = order("SO-20261001-0001", gangnam, null, StoreOrderStatus.REQUESTED, at(1),
                line(skuA, 100, 0, "1000"), line(skuB, 50, 0, "2000"));
        so2 = order("SO-20261002-0001", gangnam, seoul, StoreOrderStatus.ASSIGNED, at(2),
                line(skuA, 10, 4, "1000"));
        so3 = order("SO-20261003-0001", busanStore, busan, StoreOrderStatus.COMPLETED, at(3),
                line(skuB, 5, 5, "2000"));
    }

    private StoreOrderSearchCondition condition(StoreOrderStatus status, Long storeId, Long warehouseId,
                                                String keyword, LocalDateTime from, LocalDateTime to) {
        return StoreOrderSearchCondition.unscoped(status, storeId, warehouseId, keyword, from, to);
    }

    private StoreOrderSearchCondition noCondition() {
        return condition(null, null, null, null, null, null);
    }

    // ---------- 저장·단건 조회 ----------

    @Test
    @DisplayName("저장하면 ID가 채워지고 미배정 발주는 warehouseId가 null이다")
    void save_assignsId() {
        StoreOrder saved = storeOrderRepository.findById(so1).orElseThrow();

        assertThat(saved.getStoreOrderId()).isEqualTo(so1);
        assertThat(saved.getOrderNo()).isEqualTo("SO-20261001-0001");
        assertThat(saved.getStatus()).isEqualTo(StoreOrderStatus.REQUESTED);
        assertThat(saved.getWarehouseId()).isNull();
        assertThat(saved.getStoreId()).isEqualTo(gangnam);
        assertThat(saved.getRequestedAt()).isEqualTo(at(1));
        assertThat(saved.getCreatedBy()).isEqualTo(1L);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("ID가 있는 발주를 저장하면 상태·창고 변경이 반영된다 (승인 → 배정)")
    void save_updatesStatusAndWarehouse() {
        StoreOrder order = storeOrderRepository.findById(so1).orElseThrow();
        order.approve();
        storeOrderRepository.save(order);
        StoreOrder approved = storeOrderRepository.findById(so1).orElseThrow();
        assertThat(approved.getStatus()).isEqualTo(StoreOrderStatus.APPROVED);

        approved.assign(seoul);
        storeOrderRepository.save(approved);
        // merge로 영속성 컨텍스트의 createdAt이 비어 있을 수 있어 DB 값을 다시 읽는다 (created_at은 updatable=false)
        entityManager.flush();
        entityManager.clear();

        StoreOrder assigned = storeOrderRepository.findById(so1).orElseThrow();
        assertThat(assigned.getStatus()).isEqualTo(StoreOrderStatus.ASSIGNED);
        assertThat(assigned.getWarehouseId()).isEqualTo(seoul);
        assertThat(assigned.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("없는 발주는 비어 있다")
    void findById_absent() {
        assertThat(storeOrderRepository.findById(999_999L)).isEmpty();
        assertThat(storeOrderRepository.findByIdForUpdate(999_999L)).isEmpty();
    }

    @Test
    @DisplayName("비관적 락 조회로도 발주를 읽을 수 있다")
    void findByIdForUpdate() {
        StoreOrder order = storeOrderRepository.findByIdForUpdate(so2).orElseThrow();

        assertThat(order.getStoreOrderId()).isEqualTo(so2);
        assertThat(order.getStatus()).isEqualTo(StoreOrderStatus.ASSIGNED);
        assertThat(order.getWarehouseId()).isEqualTo(seoul);
    }

    @Test
    @DisplayName("같은 주문 번호를 저장하면 UNIQUE 제약으로 실패한다")
    void save_duplicateOrderNo_throws() {
        StoreOrder duplicate = StoreOrder.register("SO-20261001-0001", gangnam, at(5), null, null, 1L);

        assertThatThrownBy(() -> storeOrderRepository.save(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------- 항목 저장·조회 ----------

    @Test
    @DisplayName("항목은 ID 오름차순으로 조회되고 수량·단가·상태가 그대로 읽힌다")
    void findLines() {
        List<StoreOrderLine> lines = storeOrderRepository.findLinesByStoreOrderId(so1);

        assertThat(lines).hasSize(2);
        assertThat(lines).extracting(StoreOrderLine::getSkuId).containsExactly(skuA, skuB);
        StoreOrderLine first = lines.get(0);
        assertThat(first.getRequestedQuantity()).isEqualTo(100L);
        assertThat(first.getAllocatedQuantity()).isZero();
        assertThat(first.getShippedQuantity()).isZero();
        assertThat(first.getRequestedUnitSupplyPrice()).isEqualByComparingTo("1000");
        assertThat(first.getStatus()).isEqualTo(StoreOrderLineStatus.REQUESTED);
    }

    @Test
    @DisplayName("항목 락 조회는 항목 ID 오름차순이고 다른 발주의 항목은 섞이지 않는다")
    void findLinesForUpdate() {
        List<StoreOrderLine> lines = storeOrderRepository.findLinesByStoreOrderIdForUpdate(so1);

        assertThat(lines).extracting(StoreOrderLine::getSkuId).containsExactly(skuA, skuB);
        assertThat(lines).allMatch(l -> l.getStoreOrderId().equals(so1));
    }

    @Test
    @DisplayName("항목 상태 변경을 다시 저장하면 반영된다 (발주 취소 시 항목 일괄 CANCELED)")
    void saveLines_updatesStatus() {
        List<StoreOrderLine> lines = storeOrderRepository.findLinesByStoreOrderIdForUpdate(so1);
        lines.forEach(StoreOrderLine::cancel);

        storeOrderRepository.saveLines(lines);

        assertThat(storeOrderRepository.findLinesByStoreOrderId(so1))
                .extracting(StoreOrderLine::getStatus)
                .containsOnly(StoreOrderLineStatus.CANCELED);
    }

    @Test
    @DisplayName("같은 발주에 같은 SKU 항목을 두 번 저장하면 UNIQUE 제약으로 실패한다")
    void saveLines_duplicateSku_throws() {
        StoreOrderLine duplicate = StoreOrderLine.register(so1, skuA, 1, new BigDecimal("1000"));

        assertThatThrownBy(() -> storeOrderRepository.saveLines(List.of(duplicate)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------- 번호 ----------

    @Test
    @DisplayName("주문 번호 접두사로 시작하는 번호 개수를 센다")
    void countByOrderNoPrefix() {
        assertThat(storeOrderRepository.countByOrderNoPrefix("SO-20261001-")).isEqualTo(1L);
        assertThat(storeOrderRepository.countByOrderNoPrefix("SO-2026100")).isEqualTo(3L);
        assertThat(storeOrderRepository.countByOrderNoPrefix("SO-20261231-")).isZero();
    }

    @Test
    @DisplayName("주문 번호 존재 여부를 확인한다")
    void existsByOrderNo() {
        assertThat(storeOrderRepository.existsByOrderNo("SO-20261001-0001")).isTrue();
        assertThat(storeOrderRepository.existsByOrderNo("SO-20261001-9999")).isFalse();
    }

    // ---------- 목록 조회 ----------

    @Test
    @DisplayName("조건이 없으면 전체를 요청 일시 내림차순으로 돌려준다")
    void search_all_orderedByRequestedAtDesc() {
        List<StoreOrderSummary> result = queryRepository.search(noCondition());

        assertThat(result).extracting(StoreOrderSummary::storeOrderId).containsExactly(so3, so2, so1);
    }

    @Test
    @DisplayName("지점 범위(점주의 담당 지점)로 좁히고, 빈 범위면 아무것도 보이지 않는다")
    void search_byStoreScope() {
        assertThat(queryRepository.search(new StoreOrderSearchCondition(
                null, null, null, null, null, null, List.of(gangnam), null)))
                .extracting(StoreOrderSummary::storeOrderId).containsExactly(so2, so1);
        assertThat(queryRepository.search(new StoreOrderSearchCondition(
                null, null, null, null, null, null, List.of(gangnam, busanStore), null))).hasSize(3);
        assertThat(queryRepository.search(new StoreOrderSearchCondition(
                null, null, null, null, null, null, List.of(), null))).isEmpty();
    }

    @Test
    @DisplayName("창고 범위(창고 관리자의 담당 창고)로 좁히면 창고 배정 전 발주는 제외된다")
    void search_byWarehouseScope_excludesUnassigned() {
        assertThat(queryRepository.search(new StoreOrderSearchCondition(
                null, null, null, null, null, null, null, List.of(seoul))))
                .extracting(StoreOrderSummary::storeOrderId).containsExactly(so2);
        assertThat(queryRepository.search(new StoreOrderSearchCondition(
                null, null, null, null, null, null, null, List.of(seoul, busan))))
                .extracting(StoreOrderSummary::storeOrderId).containsExactly(so3, so2);
        assertThat(queryRepository.search(new StoreOrderSearchCondition(
                null, null, null, null, null, null, null, List.of()))).isEmpty();
    }

    @Test
    @DisplayName("미배정 발주는 창고 이름이 null이고 배정된 발주는 창고 이름이 채워진다 (left join)")
    void search_warehouseNullForUnassigned() {
        List<StoreOrderSummary> result = queryRepository.search(noCondition());

        StoreOrderSummary unassigned = byId(result, so1);
        assertThat(unassigned.warehouseId()).isNull();
        assertThat(unassigned.warehouseName()).isNull();
        StoreOrderSummary assigned = byId(result, so2);
        assertThat(assigned.warehouseId()).isEqualTo(seoul);
        assertThat(assigned.warehouseName()).isEqualTo("서울 물류센터");
    }

    @Test
    @DisplayName("목록 행에 지점명, 항목 수, 금액 합계, 부족 항목 수가 집계된다")
    void search_aggregates() {
        List<StoreOrderSummary> result = queryRepository.search(noCondition());

        StoreOrderSummary s1 = byId(result, so1);
        assertThat(s1.storeName()).isEqualTo("강남점");
        assertThat(s1.lineCount()).isEqualTo(2L);
        assertThat(s1.totalAmount()).isEqualByComparingTo("200000");
        assertThat(s1.shortageLineCount()).isEqualTo(2L);
        assertThat(s1.hasShortage()).isTrue();

        StoreOrderSummary s2 = byId(result, so2);
        assertThat(s2.lineCount()).isEqualTo(1L);
        assertThat(s2.totalAmount()).isEqualByComparingTo("10000");
        assertThat(s2.shortageLineCount()).isEqualTo(1L);

        StoreOrderSummary s3 = byId(result, so3);
        assertThat(s3.totalAmount()).isEqualByComparingTo("10000");
        assertThat(s3.shortageLineCount()).isZero();
        assertThat(s3.hasShortage()).isFalse();
    }

    @Test
    @DisplayName("상태·지점·창고 조건으로 거른다")
    void search_filters() {
        assertThat(queryRepository.search(condition(StoreOrderStatus.ASSIGNED, null, null, null, null, null)))
                .extracting(StoreOrderSummary::storeOrderId).containsExactly(so2);
        assertThat(queryRepository.search(condition(null, gangnam, null, null, null, null)))
                .extracting(StoreOrderSummary::storeOrderId).containsExactly(so2, so1);
        assertThat(queryRepository.search(condition(null, null, busan, null, null, null)))
                .extracting(StoreOrderSummary::storeOrderId).containsExactly(so3);
    }

    @Test
    @DisplayName("keyword는 주문 번호와 지점명을 대소문자 구분 없이 부분 일치로 찾는다")
    void search_keyword() {
        assertThat(queryRepository.search(condition(null, null, null, "20261002", null, null)))
                .extracting(StoreOrderSummary::storeOrderId).containsExactly(so2);
        assertThat(queryRepository.search(condition(null, null, null, "so-2026", null, null)))
                .extracting(StoreOrderSummary::storeOrderId).containsExactly(so3, so2, so1);
        assertThat(queryRepository.search(condition(null, null, null, "부산", null, null)))
                .extracting(StoreOrderSummary::storeOrderId).containsExactly(so3);
        assertThat(queryRepository.search(condition(null, null, null, "  강남  ", null, null)))
                .extracting(StoreOrderSummary::storeOrderId).containsExactly(so2, so1);
        assertThat(queryRepository.search(condition(null, null, null, "없는검색어", null, null))).isEmpty();
    }

    @Test
    @DisplayName("빈 keyword는 조건 없음으로 본다")
    void search_blankKeyword() {
        assertThat(queryRepository.search(condition(null, null, null, "   ", null, null))).hasSize(3);
    }

    @Test
    @DisplayName("요청 일시 범위로 거른다 (경계 포함)")
    void search_requestedRange() {
        assertThat(queryRepository.search(condition(null, null, null, null, at(2), null)))
                .extracting(StoreOrderSummary::storeOrderId).containsExactly(so3, so2);
        assertThat(queryRepository.search(condition(null, null, null, null, null, at(2))))
                .extracting(StoreOrderSummary::storeOrderId).containsExactly(so2, so1);
        assertThat(queryRepository.search(condition(null, null, null, null, at(2), at(2))))
                .extracting(StoreOrderSummary::storeOrderId).containsExactly(so2);
    }

    @Test
    @DisplayName("여러 조건은 AND로 결합한다")
    void search_combined() {
        assertThat(queryRepository.search(condition(StoreOrderStatus.REQUESTED, gangnam, null, "강남", at(1), at(3))))
                .extracting(StoreOrderSummary::storeOrderId).containsExactly(so1);
        assertThat(queryRepository.search(condition(StoreOrderStatus.COMPLETED, gangnam, null, null, null, null)))
                .isEmpty();
    }

    // ---------- 단건·항목 뷰 ----------

    @Test
    @DisplayName("발주 단건 뷰는 헤더 필드와 지점·창고 이름, 합계를 담는다")
    void findView() {
        StoreOrderView view = queryRepository.findView(so2).orElseThrow();

        assertThat(view.storeOrderId()).isEqualTo(so2);
        assertThat(view.orderNo()).isEqualTo("SO-20261002-0001");
        assertThat(view.storeName()).isEqualTo("강남점");
        assertThat(view.warehouseName()).isEqualTo("서울 물류센터");
        assertThat(view.status()).isEqualTo(StoreOrderStatus.ASSIGNED);
        assertThat(view.requestedAt()).isEqualTo(at(2));
        assertThat(view.note()).isEqualTo("비고");
        assertThat(view.lineCount()).isEqualTo(1L);
        assertThat(view.totalAmount()).isEqualByComparingTo("10000");
        assertThat(view.hasShortage()).isTrue();
        assertThat(view.createdBy()).isEqualTo(1L);
        assertThat(view.createdAt()).isNotNull();
        assertThat(view.updatedAt()).isNotNull();
    }

    @Test
    @DisplayName("미배정 발주의 단건 뷰는 창고 이름이 null이다")
    void findView_unassigned() {
        StoreOrderView view = queryRepository.findView(so1).orElseThrow();

        assertThat(view.warehouseId()).isNull();
        assertThat(view.warehouseName()).isNull();
        assertThat(view.lineCount()).isEqualTo(2L);
        assertThat(view.totalAmount()).isEqualByComparingTo("200000");
    }

    @Test
    @DisplayName("없는 발주의 단건 뷰는 비어 있다")
    void findView_absent() {
        assertThat(queryRepository.findView(999_999L)).isEmpty();
    }

    @Test
    @DisplayName("항목 뷰는 SKU 코드 오름차순이고 SKU 정보와 금액·부족 수량을 담는다")
    void findLineViews() {
        List<StoreOrderLineView> lines = queryRepository.findLineViews(so1);

        assertThat(lines).extracting(StoreOrderLineView::skuCode).containsExactly("SKU-A", "SKU-B");
        StoreOrderLineView first = lines.get(0);
        assertThat(first.skuId()).isEqualTo(skuA);
        assertThat(first.skuName()).isEqualTo("에이 상품");
        assertThat(first.unit()).isEqualTo("EA");
        assertThat(first.requestedQuantity()).isEqualTo(100L);
        assertThat(first.lineAmount()).isEqualByComparingTo("100000");
        assertThat(first.remainingQuantity()).isEqualTo(100L);
        assertThat(first.status()).isEqualTo(StoreOrderLineStatus.REQUESTED);

        StoreOrderLineView partial = queryRepository.findLineViews(so2).get(0);
        assertThat(partial.shippedQuantity()).isEqualTo(4L);
        assertThat(partial.remainingQuantity()).isEqualTo(6L);
    }

    @Test
    @DisplayName("항목이 없는 발주 번호의 항목 뷰는 비어 있다")
    void findLineViews_absent() {
        assertThat(queryRepository.findLineViews(999_999L)).isEmpty();
    }

    @Test
    @DisplayName("지점의 발주 중 주어진 상태가 하나라도 있으면 true, 다른 지점·다른 상태만 있으면 false")
    void existsByStoreIdAndStatusIn() {
        Set<StoreOrderStatus> inProgress = Set.of(
                StoreOrderStatus.REQUESTED, StoreOrderStatus.APPROVED,
                StoreOrderStatus.ASSIGNED, StoreOrderStatus.ON_HOLD);

        assertThat(storeOrderRepository.existsByStoreIdAndStatusIn(gangnam, inProgress)).isTrue();
        // 부산 지점은 COMPLETED 발주만 있다
        assertThat(storeOrderRepository.existsByStoreIdAndStatusIn(busanStore, inProgress)).isFalse();
        assertThat(storeOrderRepository.existsByStoreIdAndStatusIn(busanStore, Set.of(StoreOrderStatus.COMPLETED)))
                .isTrue();
        assertThat(storeOrderRepository.existsByStoreIdAndStatusIn(999_999L, inProgress)).isFalse();
    }

    // ---------- 헬퍼 ----------

    private LocalDateTime at(int day) {
        return LocalDateTime.of(2026, 10, day, 9, 0);
    }

    private StoreOrderSummary byId(List<StoreOrderSummary> list, Long id) {
        return list.stream().filter(s -> s.storeOrderId().equals(id)).findFirst().orElseThrow();
    }

    private Long store(String code, String name) {
        return storeJpaRepository.save(StoreJpaEntity.builder()
                .storeCode(code).name(name).address("주소").contactNumber("02-1234-5678")
                .status(StoreStatus.ACTIVE)
                .build()).getStoreId();
    }

    private Long warehouse(String code, String name) {
        return warehouseJpaRepository.save(WarehouseJpaEntity.builder()
                .warehouseCode(code).name(name).address("주소").totalCapacity(BigDecimal.valueOf(1000))
                .status(WarehouseStatus.ACTIVE)
                .build()).getWarehouseId();
    }

    private Long sku(String code, String name) {
        return productSkuJpaRepository.save(ProductSkuJpaEntity.builder()
                .productId(1L).skuCode(code).name(name).unit("EA")
                .safetyStockQuantity(0L).status(ProductStatus.ACTIVE)
                .build()).getSkuId();
    }

    /** 아직 발주 ID가 없는 항목(주문 저장 시 ID를 채운다). */
    private StoreOrderLine line(Long skuId, long requested, long shipped, String price) {
        return StoreOrderLine.builder()
                .skuId(skuId).requestedQuantity(requested).shippedQuantity(shipped)
                .requestedUnitSupplyPrice(new BigDecimal(price))
                .status(StoreOrderLineStatus.REQUESTED)
                .build();
    }

    private Long order(String no, Long storeId, Long warehouseId, StoreOrderStatus status,
                       LocalDateTime requestedAt, StoreOrderLine... lines) {
        StoreOrder order = StoreOrder.builder()
                .orderNo(no).storeId(storeId).warehouseId(warehouseId).status(status)
                .requestedAt(requestedAt).note("비고").createdBy(1L)
                .build();
        Long id = storeOrderRepository.save(order).getStoreOrderId();
        List<StoreOrderLine> withId = Arrays.stream(lines)
                .map(l -> StoreOrderLine.builder()
                        .storeOrderId(id).skuId(l.getSkuId())
                        .requestedQuantity(l.getRequestedQuantity()).shippedQuantity(l.getShippedQuantity())
                        .requestedUnitSupplyPrice(l.getRequestedUnitSupplyPrice())
                        .status(l.getStatus())
                        .build())
                .toList();
        storeOrderRepository.saveLines(withId);
        return id;
    }

    @Test
    @DisplayName("작성자 이름을 사용자 테이블 조인으로 단건 뷰에 담고, 사용자가 없으면 이름만 null이다")
    void findView_createdByName() {
        User writer = userRepository.save(User.signUp("so_writer", "hashed", "김점주", "so_writer@example.com",
                "010-1234-5678", UserRole.STORE_OWNER));
        entityManager.createQuery("update StoreOrderJpaEntity o set o.createdBy = :userId where o.storeOrderId = :id")
                .setParameter("userId", writer.getUserId()).setParameter("id", so2).executeUpdate();
        // 방금 만든 사용자와 절대 겹치지 않는 존재하지 않는 사용자 ID
        long nobody = writer.getUserId() + 1_000_000L;
        entityManager.createQuery("update StoreOrderJpaEntity o set o.createdBy = :userId where o.storeOrderId = :id")
                .setParameter("userId", nobody).setParameter("id", so1).executeUpdate();
        entityManager.clear();

        assertThat(queryRepository.findView(so2).orElseThrow().createdByName()).isEqualTo("김점주");
        StoreOrderView other = queryRepository.findView(so1).orElseThrow();
        assertThat(other.createdBy()).isEqualTo(nobody);
        assertThat(other.createdByName()).isNull();
    }
}
