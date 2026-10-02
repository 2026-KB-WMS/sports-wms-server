package com.kb.wms.inbound.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.inbound.application.port.in.query.InboundSearchCondition;
import com.kb.wms.inbound.application.port.in.query.SectionCandidateCondition;
import com.kb.wms.inbound.application.port.in.result.InboundLineView;
import com.kb.wms.inbound.application.port.in.result.InboundSummary;
import com.kb.wms.inbound.application.port.in.result.InboundView;
import com.kb.wms.inbound.application.port.in.result.SectionCandidate;
import com.kb.wms.inbound.application.port.out.InboundQueryRepository;
import com.kb.wms.inbound.application.port.out.InboundRepository;
import com.kb.wms.inbound.application.port.out.PurchaseOrderRepository;
import com.kb.wms.inbound.application.port.out.SupplierRepository;
import com.kb.wms.inbound.domain.entity.Inbound;
import com.kb.wms.inbound.domain.entity.InboundLine;
import com.kb.wms.inbound.domain.entity.PurchaseOrder;
import com.kb.wms.inbound.domain.entity.PurchaseOrderLine;
import com.kb.wms.inbound.domain.entity.Supplier;
import com.kb.wms.inbound.domain.enums.InboundStatus;
import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;
import com.kb.wms.inventory.application.port.out.LotRepository;
import com.kb.wms.inventory.domain.entity.Lot;
import com.kb.wms.product.adapter.out.persistence.entity.ProductSkuJpaEntity;
import com.kb.wms.product.adapter.out.persistence.repository.ProductSkuJpaRepository;
import com.kb.wms.product.domain.enums.ProductStatus;
import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseJpaEntity;
import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseSectionJpaEntity;
import com.kb.wms.warehouse.adapter.out.persistence.repository.WarehouseJpaRepository;
import com.kb.wms.warehouse.adapter.out.persistence.repository.WarehouseSectionJpaRepository;
import com.kb.wms.warehouse.domain.enums.WarehouseStatus;

/**
 * 입고 영속성 어댑터(저장·조회 전용 쿼리) 검증.
 *
 * <p>데이터: 창고 2개(서울/부산), 공급처 1곳, SKU 2개(SKU-A, SKU-B), 로트 2개(LOT-A, LOT-B)
 * <ul>
 *   <li>PO1: 서울 / SKU-A 100 + SKU-B 50, PO2: 부산 / SKU-A 10, PO3: 서울 / SKU-B 5 (모두 CONFIRMED)</li>
 *   <li>IB1: PO1 / 서울 / COMPLETED / 이틀 전 도착 / 검수 항목 2줄(SKU-A, SKU-B)</li>
 *   <li>IB2: PO1 / 서울 / INSPECTING / 하루 전 도착 / 검수 항목 1줄</li>
 *   <li>IB3: PO2 / 부산 / ARRIVED / 현재 도착 / 검수 항목 없음</li>
 *   <li>IB4: PO3 / 서울 / CANCELED / 현재 도착 / 검수 항목 없음</li>
 * </ul>
 * 서울 구역: 활성 RACK 2개(여유 있음/가득 참), 비활성 RACK, 활성 DEFECT 2개(여유 있음/가득 참). 부산에는 활성 RACK 1개.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "spring.flyway.enabled=false")
class InboundPersistenceAdapterTest {

    private static final LocalDate MANUFACTURED = LocalDate.of(2026, 1, 1);
    private static final LocalDate EXPIRY = LocalDate.of(2027, 1, 1);

    @Autowired InboundRepository inboundRepository;
    @Autowired InboundQueryRepository queryRepository;
    @Autowired PurchaseOrderRepository purchaseOrderRepository;
    @Autowired SupplierRepository supplierRepository;
    @Autowired LotRepository lotRepository;
    @Autowired WarehouseJpaRepository warehouseJpaRepository;
    @Autowired WarehouseSectionJpaRepository warehouseSectionJpaRepository;
    @Autowired ProductSkuJpaRepository productSkuJpaRepository;

    Long seoul;
    Long busan;
    Long supplier;
    Long skuA;
    Long skuB;
    Long lotA;
    Long lotB;
    Long rackOk;
    Long rackFull;
    Long rackInactive;
    Long defectOk;
    Long defectFull;
    Long busanRack;
    Long po1;
    Long po2;
    Long po3;
    Long po1LineA;
    Long po1LineB;
    Long ib1;
    Long ib2;
    Long ib3;
    Long ib4;
    LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now().withNano(0);

        seoul = warehouse("WH-IBP-SEOUL", "서울 물류센터");
        busan = warehouse("WH-IBP-BUSAN", "부산 물류센터");
        supplier = supplierRepository.save(
                Supplier.register("SUP-IBP", "가나 상사", "담당자", "02-1234-5678", "a@b.com", "주소")).getSupplierId();
        skuA = sku("SKU-IBP-A", "에이 상품");
        skuB = sku("SKU-IBP-B", "비 상품");
        lotA = lotRepository.save(Lot.register(skuA, 1L, "LOT-IBP-A", MANUFACTURED, EXPIRY, BigDecimal.TEN)).getLotId();
        lotB = lotRepository.save(Lot.register(skuB, 1L, "LOT-IBP-B", null, null, BigDecimal.TEN)).getLotId();

        rackOk = section(seoul, "IBP-R1", "A동 1랙", "RACK", 100, 40, WarehouseStatus.ACTIVE);
        rackFull = section(seoul, "IBP-R2", "A동 2랙", "RACK", 50, 50, WarehouseStatus.ACTIVE);
        rackInactive = section(seoul, "IBP-R3", "A동 3랙", "RACK", 100, 0, WarehouseStatus.INACTIVE);
        defectOk = section(seoul, "IBP-D1", "불량 1구역", "DEFECT", 20, 5, WarehouseStatus.ACTIVE);
        defectFull = section(seoul, "IBP-D2", "불량 2구역", "DEFECT", 10, 10, WarehouseStatus.ACTIVE);
        busanRack = section(busan, "IBP-B1", "부산 1랙", "RACK", 100, 0, WarehouseStatus.ACTIVE);

        po1 = order("PO-IBP-0001", seoul);
        po2 = order("PO-IBP-0002", busan);
        po3 = order("PO-IBP-0003", seoul);
        po1LineA = orderLine(po1, skuA, 100, "1000");
        po1LineB = orderLine(po1, skuB, 50, "2000");
        orderLine(po2, skuA, 10, "1000");
        orderLine(po3, skuB, 5, "2000");

        ib1 = inbound("IB-IBP-0001", po1, seoul, InboundStatus.COMPLETED, now.minusDays(2), "완료 입고");
        ib2 = inbound("IB-IBP-0002", po1, seoul, InboundStatus.INSPECTING, now.minusDays(1), null);
        ib3 = inbound("IB-IBP-0003", po2, busan, InboundStatus.ARRIVED, now, null);
        ib4 = inbound("IB-IBP-0004", po3, seoul, InboundStatus.CANCELED, now, null);

        inboundRepository.saveLines(List.of(
                inboundLine(ib1, po1LineA, lotA, rackOk, defectOk, 10, 8, 2, "1000", "단가 인상"),
                inboundLine(ib1, po1LineB, lotB, rackOk, null, 5, 5, 0, "2000", null)));
        inboundRepository.saveLines(List.of(
                inboundLine(ib2, po1LineA, lotA, null, null, 3, 3, 0, "1000", null)));
    }

    // ---------- 저장·조회 ----------

    @Test
    @DisplayName("저장한 입고 헤더와 검수 항목을 다시 조회할 수 있다")
    void saveAndFind() {
        Inbound found = inboundRepository.findById(ib1).orElseThrow();

        assertThat(found.getInboundNo()).isEqualTo("IB-IBP-0001");
        assertThat(found.getPurchaseOrderId()).isEqualTo(po1);
        assertThat(found.getWarehouseId()).isEqualTo(seoul);
        assertThat(found.getStatus()).isEqualTo(InboundStatus.COMPLETED);
        assertThat(found.getReceivedBy()).isEqualTo(7L);
        assertThat(found.getNote()).isEqualTo("완료 입고");

        List<InboundLine> lines = inboundRepository.findLinesByInboundId(ib1);
        assertThat(lines).extracting(InboundLine::getPurchaseOrderLineId).containsExactly(po1LineA, po1LineB);
        assertThat(lines.get(0).getAcceptedQuantity()).isEqualTo(8L);
        assertThat(lines.get(0).getDefectiveQuantity()).isEqualTo(2L);
        assertThat(lines.get(0).getAcceptedSectionId()).isEqualTo(rackOk);
        assertThat(lines.get(0).getDefectSectionId()).isEqualTo(defectOk);
        assertThat(lines.get(0).getPriceChangeReason()).isEqualTo("단가 인상");
        assertThat(lines.get(1).getDefectSectionId()).isNull();
    }

    @Test
    @DisplayName("없는 입고는 빈 값을 돌려준다")
    void findById_notFound() {
        assertThat(inboundRepository.findById(999_999L)).isEmpty();
        assertThat(inboundRepository.findByIdForUpdate(999_999L)).isEmpty();
        assertThat(queryRepository.findView(999_999L)).isEmpty();
        assertThat(inboundRepository.findLinesByInboundId(999_999L)).isEmpty();
    }

    @Test
    @DisplayName("비관적 락 조회도 같은 입고를 돌려준다")
    void findByIdForUpdate() {
        assertThat(inboundRepository.findByIdForUpdate(ib2).orElseThrow().getStatus())
                .isEqualTo(InboundStatus.INSPECTING);
    }

    @Test
    @DisplayName("상태 변경을 저장하면 반영된다")
    void saveStatusChange() {
        Inbound inbound = inboundRepository.findById(ib2).orElseThrow();
        inbound.complete(9L, now);
        inboundRepository.save(inbound);

        Inbound found = inboundRepository.findById(ib2).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(InboundStatus.COMPLETED);
        assertThat(found.getReceivedBy()).isEqualTo(9L);
    }

    @Test
    @DisplayName("입고의 검수 항목을 모두 지운다")
    void deleteLinesByInboundId() {
        inboundRepository.deleteLinesByInboundId(ib1);

        assertThat(inboundRepository.findLinesByInboundId(ib1)).isEmpty();
        assertThat(inboundRepository.findLinesByInboundId(ib2)).hasSize(1);
    }

    @Test
    @DisplayName("입고 번호 존재 여부와 접두어 개수를 센다")
    void numberQueries() {
        assertThat(inboundRepository.existsByInboundNo("IB-IBP-0001")).isTrue();
        assertThat(inboundRepository.existsByInboundNo("IB-IBP-9999")).isFalse();

        assertThat(inboundRepository.countByInboundNoPrefix("IB-IBP-")).isEqualTo(4);
        assertThat(inboundRepository.countByInboundNoPrefix("IB-IBP-000")).isEqualTo(4);
        assertThat(inboundRepository.countByInboundNoPrefix("IB-NONE-")).isZero();
    }

    @Test
    @DisplayName("발주의 진행 중 입고 여부는 ARRIVED·INSPECTING만 true다")
    void existsInProgressByPurchaseOrderId() {
        assertThat(inboundRepository.existsInProgressByPurchaseOrderId(po1)).isTrue();   // COMPLETED + INSPECTING
        assertThat(inboundRepository.existsInProgressByPurchaseOrderId(po2)).isTrue();   // ARRIVED
        assertThat(inboundRepository.existsInProgressByPurchaseOrderId(po3)).isFalse();  // CANCELED만

        Inbound inbound = inboundRepository.findById(ib2).orElseThrow();
        inbound.complete(7L, now);
        inboundRepository.save(inbound);
        assertThat(inboundRepository.existsInProgressByPurchaseOrderId(po1)).isFalse();  // COMPLETED만
    }

    @Test
    @DisplayName("발주의 취소되지 않은 입고 여부는 CANCELED만 있으면 false다")
    void existsNotCanceledByPurchaseOrderId() {
        assertThat(inboundRepository.existsNotCanceledByPurchaseOrderId(po1)).isTrue();
        assertThat(inboundRepository.existsNotCanceledByPurchaseOrderId(po2)).isTrue();
        assertThat(inboundRepository.existsNotCanceledByPurchaseOrderId(po3)).isFalse();

        Long poWithoutInbound = order("PO-IBP-0099", seoul);
        assertThat(inboundRepository.existsNotCanceledByPurchaseOrderId(poWithoutInbound)).isFalse();
    }

    // ---------- search ----------

    @Test
    @DisplayName("조건이 없으면 전체 입고를 발주·공급처·창고 정보와 검수 항목 수 집계와 함께 조회한다")
    void search_all() {
        List<InboundSummary> result = queryRepository.search(condition(null, null, null, null, null, null));

        assertThat(result).extracting(InboundSummary::inboundId).containsExactlyInAnyOrder(ib1, ib2, ib3, ib4);

        InboundSummary s1 = byId(result, ib1);
        assertThat(s1.inboundNo()).isEqualTo("IB-IBP-0001");
        assertThat(s1.purchaseOrderId()).isEqualTo(po1);
        assertThat(s1.purchaseOrderNo()).isEqualTo("PO-IBP-0001");
        assertThat(s1.supplierId()).isEqualTo(supplier);
        assertThat(s1.supplierName()).isEqualTo("가나 상사");
        assertThat(s1.warehouseId()).isEqualTo(seoul);
        assertThat(s1.warehouseName()).isEqualTo("서울 물류센터");
        assertThat(s1.status()).isEqualTo(InboundStatus.COMPLETED);
        assertThat(s1.receivedBy()).isEqualTo(7L);
        assertThat(s1.lineCount()).isEqualTo(2L);

        assertThat(byId(result, ib2).lineCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("검수 항목이 없는 입고도 항목 수 0으로 목록에 나온다")
    void search_includesInboundWithoutLines() {
        List<InboundSummary> result = queryRepository.search(condition(null, null, null, null, null, null));

        assertThat(byId(result, ib3).lineCount()).isZero();
        assertThat(byId(result, ib4).lineCount()).isZero();
    }

    @Test
    @DisplayName("도착 일시 최신순(같으면 입고 ID 내림차순)으로 정렬된다")
    void search_orderedByLatestArrival() {
        List<InboundSummary> result = queryRepository.search(condition(null, null, null, null, null, null));

        // IB3·IB4는 도착 일시가 같으므로 ID 내림차순(IB4 → IB3), 그 뒤로 IB2(하루 전), IB1(이틀 전)
        assertThat(result).extracting(InboundSummary::inboundId).containsExactly(ib4, ib3, ib2, ib1);
        assertThat(result).extracting(InboundSummary::arrivedAt)
                .isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test
    @DisplayName("상태로 필터링한다")
    void search_byStatus() {
        assertThat(queryRepository.search(condition(InboundStatus.INSPECTING, null, null, null, null, null)))
                .extracting(InboundSummary::inboundId).containsExactly(ib2);
        assertThat(queryRepository.search(condition(InboundStatus.CANCELED, null, null, null, null, null)))
                .extracting(InboundSummary::inboundId).containsExactly(ib4);
    }

    @Test
    @DisplayName("창고·발주로 필터링한다")
    void search_byWarehouseAndPurchaseOrder() {
        assertThat(queryRepository.search(condition(null, seoul, null, null, null, null)))
                .extracting(InboundSummary::inboundId).containsExactlyInAnyOrder(ib1, ib2, ib4);
        assertThat(queryRepository.search(condition(null, null, po1, null, null, null)))
                .extracting(InboundSummary::inboundId).containsExactlyInAnyOrder(ib1, ib2);
        assertThat(queryRepository.search(condition(null, busan, po1, null, null, null))).isEmpty();
    }

    @Test
    @DisplayName("키워드는 입고 번호와 발주 번호를 대소문자 구분 없이 부분 일치로 찾는다")
    void search_byKeyword() {
        assertThat(queryRepository.search(condition(null, null, null, "ib-ibp-0003", null, null)))
                .extracting(InboundSummary::inboundId).containsExactly(ib3);
        assertThat(queryRepository.search(condition(null, null, null, "po-ibp-0001", null, null)))
                .extracting(InboundSummary::inboundId).containsExactlyInAnyOrder(ib1, ib2);
        assertThat(queryRepository.search(condition(null, null, null, "NOPE", null, null))).isEmpty();
    }

    @Test
    @DisplayName("도착 일시 범위로 필터링한다")
    void search_byArrivedRange() {
        assertThat(queryRepository.search(condition(null, null, null, null, now.minusDays(1).minusHours(1), null)))
                .extracting(InboundSummary::inboundId).containsExactlyInAnyOrder(ib2, ib3, ib4);
        assertThat(queryRepository.search(condition(null, null, null, null, null, now.minusDays(1).plusHours(1))))
                .extracting(InboundSummary::inboundId).containsExactlyInAnyOrder(ib1, ib2);
        assertThat(queryRepository.search(condition(null, null, null, null, now.plusDays(1), null))).isEmpty();
    }

    // ---------- view ----------

    @Test
    @DisplayName("입고 상세 헤더를 발주 상태·공급처·창고·항목 수와 함께 조회한다")
    void findView() {
        InboundView view = queryRepository.findView(ib1).orElseThrow();

        assertThat(view.inboundNo()).isEqualTo("IB-IBP-0001");
        assertThat(view.purchaseOrderNo()).isEqualTo("PO-IBP-0001");
        assertThat(view.purchaseOrderStatus()).isEqualTo(PurchaseOrderStatus.CONFIRMED);
        assertThat(view.supplierName()).isEqualTo("가나 상사");
        assertThat(view.warehouseName()).isEqualTo("서울 물류센터");
        assertThat(view.status()).isEqualTo(InboundStatus.COMPLETED);
        assertThat(view.note()).isEqualTo("완료 입고");
        assertThat(view.receivedBy()).isEqualTo(7L);
        assertThat(view.lineCount()).isEqualTo(2L);
        assertThat(view.createdAt()).isNotNull();
    }

    @Test
    @DisplayName("검수 항목이 없는 입고의 상세도 조회된다")
    void findView_withoutLines() {
        InboundView view = queryRepository.findView(ib3).orElseThrow();

        assertThat(view.status()).isEqualTo(InboundStatus.ARRIVED);
        assertThat(view.warehouseName()).isEqualTo("부산 물류센터");
        assertThat(view.lineCount()).isZero();
        assertThat(view.receivedAt()).isNull();
    }

    // ---------- line views ----------

    @Test
    @DisplayName("검수 항목 뷰는 SKU 코드·로트 번호순으로 나오고 SKU·로트·구역·단가 정보를 붙인다")
    void findLineViews() {
        List<InboundLineView> lines = queryRepository.findLineViews(ib1);

        assertThat(lines).extracting(InboundLineView::skuCode).containsExactly("SKU-IBP-A", "SKU-IBP-B");

        InboundLineView a = lines.get(0);
        assertThat(a.skuId()).isEqualTo(skuA);
        assertThat(a.skuName()).isEqualTo("에이 상품");
        assertThat(a.lotId()).isEqualTo(lotA);
        assertThat(a.lotNumber()).isEqualTo("LOT-IBP-A");
        assertThat(a.manufacturedDate()).isEqualTo(MANUFACTURED);
        assertThat(a.expiryDate()).isEqualTo(EXPIRY);
        assertThat(a.receivedQuantity()).isEqualTo(10L);
        assertThat(a.acceptedQuantity()).isEqualTo(8L);
        assertThat(a.defectiveQuantity()).isEqualTo(2L);
        assertThat(a.acceptedSectionId()).isEqualTo(rackOk);
        assertThat(a.acceptedSectionCode()).isEqualTo("IBP-R1");
        assertThat(a.defectSectionId()).isEqualTo(defectOk);
        assertThat(a.defectSectionCode()).isEqualTo("IBP-D1");
        assertThat(a.orderedUnitPrice()).isEqualByComparingTo("1000");
        assertThat(a.receivedUnitPrice()).isEqualByComparingTo("1000");
        assertThat(a.lineAmount()).isEqualByComparingTo("10000");
        assertThat(a.priceChangeReason()).isEqualTo("단가 인상");
        assertThat(a.receivedBy()).isEqualTo(7L);
    }

    @Test
    @DisplayName("구역을 아직 지정하지 않은 검수 항목도 구역 정보만 비어서 조회된다")
    void findLineViews_withoutSections() {
        List<InboundLineView> lines = queryRepository.findLineViews(ib2);

        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).acceptedSectionId()).isNull();
        assertThat(lines.get(0).acceptedSectionCode()).isNull();
        assertThat(lines.get(0).defectSectionId()).isNull();
        assertThat(lines.get(0).defectSectionCode()).isNull();
        assertThat(lines.get(0).manufacturedDate()).isEqualTo(MANUFACTURED);
    }

    @Test
    @DisplayName("같은 SKU를 로트별로 나눠 받으면 로트 번호순으로 여러 줄이 나온다")
    void findLineViews_splitLots() {
        Long lotA2 = lotRepository.save(
                Lot.register(skuA, 1L, "LOT-IBP-A0", MANUFACTURED, EXPIRY, BigDecimal.TEN)).getLotId();
        inboundRepository.saveLines(List.of(
                inboundLine(ib3, po1LineA, lotA, rackOk, null, 4, 4, 0, "1000", null),
                inboundLine(ib3, po1LineA, lotA2, rackOk, null, 6, 6, 0, "1000", null)));

        List<InboundLineView> lines = queryRepository.findLineViews(ib3);

        assertThat(lines).extracting(InboundLineView::lotNumber).containsExactly("LOT-IBP-A", "LOT-IBP-A0");
        assertThat(lines).extracting(InboundLineView::receivedQuantity).containsExactly(4L, 6L);
    }

    @Test
    @DisplayName("검수 항목이 없는 입고는 빈 목록을 돌려준다")
    void findLineViews_empty() {
        assertThat(queryRepository.findLineViews(ib3)).isEmpty();
    }

    // ---------- section candidates ----------

    @Test
    @DisplayName("합격품 구역 후보는 활성이고 DEFECT가 아니며 여유 용량이 있는 구역이다")
    void findAssignableSections() {
        List<SectionCandidate> result = queryRepository.findAssignableSections(
                seoul, new SectionCandidateCondition(null, null));

        assertThat(result).extracting(SectionCandidate::sectionId).containsExactly(rackOk);
        SectionCandidate candidate = result.get(0);
        assertThat(candidate.sectionCode()).isEqualTo("IBP-R1");
        assertThat(candidate.sectionName()).isEqualTo("A동 1랙");
        assertThat(candidate.sectionType()).isEqualTo("RACK");
        assertThat(candidate.capacity()).isEqualByComparingTo("100");
        assertThat(candidate.currentCapacity()).isEqualByComparingTo("40");
        assertThat(candidate.availableCapacity()).isEqualByComparingTo("60");
    }

    @Test
    @DisplayName("합격품 구역 후보는 다른 창고의 구역을 포함하지 않는다")
    void findAssignableSections_otherWarehouse() {
        assertThat(queryRepository.findAssignableSections(busan, new SectionCandidateCondition(null, null)))
                .extracting(SectionCandidate::sectionId).containsExactly(busanRack);
    }

    @Test
    @DisplayName("필요 수량 이상의 여유가 있는 구역만 후보가 된다")
    void findAssignableSections_requiredQuantity() {
        assertThat(queryRepository.findAssignableSections(
                seoul, new SectionCandidateCondition(BigDecimal.valueOf(60), null)))
                .extracting(SectionCandidate::sectionId).containsExactly(rackOk);
        assertThat(queryRepository.findAssignableSections(
                seoul, new SectionCandidateCondition(BigDecimal.valueOf(61), null))).isEmpty();
    }

    @Test
    @DisplayName("구역 후보 키워드는 구역명·코드를 대소문자 구분 없이 부분 일치로 찾는다")
    void findAssignableSections_keyword() {
        assertThat(queryRepository.findAssignableSections(
                seoul, new SectionCandidateCondition(null, "ibp-r1")))
                .extracting(SectionCandidate::sectionId).containsExactly(rackOk);
        assertThat(queryRepository.findAssignableSections(
                seoul, new SectionCandidateCondition(null, "1랙")))
                .extracting(SectionCandidate::sectionId).containsExactly(rackOk);
        assertThat(queryRepository.findAssignableSections(
                seoul, new SectionCandidateCondition(null, "없는구역"))).isEmpty();
    }

    @Test
    @DisplayName("구역 후보는 구역 코드순으로 정렬된다")
    void findAssignableSections_orderedByCode() {
        Long extra = section(seoul, "IBP-R0", "A동 0랙", "RACK", 100, 0, WarehouseStatus.ACTIVE);

        assertThat(queryRepository.findAssignableSections(seoul, new SectionCandidateCondition(null, null)))
                .extracting(SectionCandidate::sectionId).containsExactly(extra, rackOk);
    }

    @Test
    @DisplayName("불량 구역 후보는 활성 DEFECT 구역 중 여유 용량이 있는 구역이다")
    void findDefectSections() {
        List<SectionCandidate> result = queryRepository.findDefectSections(
                seoul, new SectionCandidateCondition(null, null));

        assertThat(result).extracting(SectionCandidate::sectionId).containsExactly(defectOk);
        assertThat(result.get(0).sectionType()).isEqualTo("DEFECT");
        assertThat(result.get(0).availableCapacity()).isEqualByComparingTo("15");
    }

    @Test
    @DisplayName("불량 구역 후보도 필요 수량·키워드로 걸러지고 다른 창고는 제외된다")
    void findDefectSections_filters() {
        assertThat(queryRepository.findDefectSections(
                seoul, new SectionCandidateCondition(BigDecimal.valueOf(15), null)))
                .extracting(SectionCandidate::sectionId).containsExactly(defectOk);
        assertThat(queryRepository.findDefectSections(
                seoul, new SectionCandidateCondition(BigDecimal.valueOf(16), null))).isEmpty();
        assertThat(queryRepository.findDefectSections(
                seoul, new SectionCandidateCondition(null, "불량 1"))).hasSize(1);
        assertThat(queryRepository.findDefectSections(
                seoul, new SectionCandidateCondition(null, "불량 2"))).isEmpty();   // 가득 찬 구역
        assertThat(queryRepository.findDefectSections(
                busan, new SectionCandidateCondition(null, null))).isEmpty();
    }

    // ---------- fixtures ----------

    private InboundSearchCondition condition(InboundStatus status, Long warehouseId, Long purchaseOrderId,
                                             String keyword, LocalDateTime from, LocalDateTime to) {
        return new InboundSearchCondition(status, warehouseId, purchaseOrderId, keyword, from, to);
    }

    private InboundSummary byId(List<InboundSummary> list, Long id) {
        return list.stream().filter(s -> s.inboundId().equals(id)).findFirst().orElseThrow();
    }

    private Long warehouse(String code, String name) {
        return warehouseJpaRepository.save(WarehouseJpaEntity.builder()
                .warehouseCode(code).name(name).address("주소").totalCapacity(BigDecimal.valueOf(100000))
                .status(WarehouseStatus.ACTIVE)
                .build()).getWarehouseId();
    }

    private Long section(Long warehouseId, String code, String name, String type,
                         long capacity, long currentCapacity, WarehouseStatus status) {
        return warehouseSectionJpaRepository.save(WarehouseSectionJpaEntity.builder()
                .warehouseId(warehouseId).sectionCode(code).name(name).sectionType(type)
                .capacity(BigDecimal.valueOf(capacity)).currentCapacity(BigDecimal.valueOf(currentCapacity))
                .status(status)
                .build()).getSectionId();
    }

    private Long sku(String code, String name) {
        return productSkuJpaRepository.save(ProductSkuJpaEntity.builder()
                .productId(1L).skuCode(code).name(name).unit("EA")
                .safetyStockQuantity(0L).status(ProductStatus.ACTIVE)
                .build()).getSkuId();
    }

    private Long order(String no, Long warehouseId) {
        return purchaseOrderRepository.save(PurchaseOrder.builder()
                .purchaseOrderNo(no).warehouseId(warehouseId).supplierId(supplier)
                .status(PurchaseOrderStatus.CONFIRMED).createdBy(1L)
                .build()).getPurchaseOrderId();
    }

    private Long orderLine(Long purchaseOrderId, Long skuId, long quantity, String price) {
        return purchaseOrderRepository.saveLines(List.of(
                PurchaseOrderLine.register(purchaseOrderId, skuId, quantity, new BigDecimal(price))))
                .get(0).getPurchaseOrderLineId();
    }

    private Long inbound(String no, Long purchaseOrderId, Long warehouseId, InboundStatus status,
                         LocalDateTime arrivedAt, String note) {
        boolean completed = status == InboundStatus.COMPLETED;
        return inboundRepository.save(Inbound.builder()
                .inboundNo(no).purchaseOrderId(purchaseOrderId).warehouseId(warehouseId)
                .status(status).arrivedAt(arrivedAt).note(note)
                .receivedAt(completed ? arrivedAt.plusHours(1) : null)
                .receivedBy(completed ? 7L : null)
                .build()).getInboundId();
    }

    private InboundLine inboundLine(Long inboundId, Long purchaseOrderLineId, Long lotId,
                                    Long acceptedSectionId, Long defectSectionId,
                                    long received, long accepted, long defective,
                                    String price, String priceChangeReason) {
        return InboundLine.register(inboundId, purchaseOrderLineId, lotId, acceptedSectionId, defectSectionId,
                received, accepted, defective, new BigDecimal(price), priceChangeReason, null, now, 7L);
    }
}
