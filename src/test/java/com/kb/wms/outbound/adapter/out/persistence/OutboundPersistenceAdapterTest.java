package com.kb.wms.outbound.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.outbound.application.port.out.OutboundRepository;
import com.kb.wms.outbound.application.port.out.StockAllocationRepository;
import com.kb.wms.outbound.domain.entity.Outbound;
import com.kb.wms.outbound.domain.entity.OutboundLine;
import com.kb.wms.outbound.domain.entity.StockAllocation;
import com.kb.wms.outbound.domain.enums.AllocationStatus;
import com.kb.wms.outbound.domain.enums.OutboundStatus;
import com.kb.wms.storeorder.adapter.out.persistence.entity.StoreOrderJpaEntity;
import com.kb.wms.storeorder.adapter.out.persistence.entity.StoreOrderLineJpaEntity;
import com.kb.wms.storeorder.adapter.out.persistence.repository.StoreOrderJpaRepository;
import com.kb.wms.storeorder.adapter.out.persistence.repository.StoreOrderLineJpaRepository;
import com.kb.wms.storeorder.domain.enums.StoreOrderLineStatus;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

/**
 * 출고·재고 할당 영속성 어댑터(저장·락 조회·연결 여부 쿼리·채번) 검증.
 * 발주·항목은 JPA 엔티티로 직접 만들고, 다른 도메인과는 ID로만 연결한다.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "spring.flyway.enabled=false")
class OutboundPersistenceAdapterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 5, 9, 0);

    @Autowired OutboundRepository outboundRepository;
    @Autowired StockAllocationRepository allocationRepository;
    @Autowired StoreOrderJpaRepository storeOrderJpaRepository;
    @Autowired StoreOrderLineJpaRepository storeOrderLineJpaRepository;

    Long orderId;
    Long otherOrderId;
    Long lineId;
    Long otherLineId;

    @BeforeEach
    void setUp() {
        orderId = order("SO-20261005-0001");
        otherOrderId = order("SO-20261005-0002");
        lineId = line(orderId, 101L);
        otherLineId = line(otherOrderId, 102L);
    }

    private Long order(String orderNo) {
        return storeOrderJpaRepository.save(StoreOrderJpaEntity.builder()
                .orderNo(orderNo)
                .storeId(1L)
                .warehouseId(1L)
                .status(StoreOrderStatus.ASSIGNED)
                .requestedAt(NOW)
                .createdBy(1L)
                .build()).getStoreOrderId();
    }

    private Long line(Long storeOrderId, Long skuId) {
        return storeOrderLineJpaRepository.save(StoreOrderLineJpaEntity.builder()
                .storeOrderId(storeOrderId)
                .skuId(skuId)
                .requestedQuantity(10L)
                .allocatedQuantity(0L)
                .shippedQuantity(0L)
                .requestedUnitSupplyPrice(new BigDecimal("1000"))
                .status(StoreOrderLineStatus.REQUESTED)
                .build()).getStoreOrderLineId();
    }

    private StockAllocation allocation(Long storeOrderLineId, Long lotId, long quantity) {
        return allocationRepository.save(StockAllocation.allocate(storeOrderLineId, lotId, quantity, 1L, NOW));
    }

    private Outbound outbound(String no, Long storeOrderId, Long... allocationIds) {
        Outbound saved = outboundRepository.save(Outbound.create(no, storeOrderId, null));
        outboundRepository.saveLines(java.util.Arrays.stream(allocationIds)
                .map(id -> OutboundLine.create(saved.getOutboundId(), id))
                .toList());
        return saved;
    }

    @Test
    @DisplayName("할당을 저장하면 ID가 채워지고 다시 읽을 수 있다")
    void saveAllocation() {
        StockAllocation saved = allocation(lineId, 11L, 5);

        StockAllocation found = allocationRepository.findById(saved.getAllocationId()).orElseThrow();

        assertThat(saved.getAllocationId()).isNotNull();
        assertThat(found.getStatus()).isEqualTo(AllocationStatus.ALLOCATED);
        assertThat(found.getAllocatedQuantity()).isEqualTo(5);
        assertThat(found.getStoreOrderLineId()).isEqualTo(lineId);
    }

    @Test
    @DisplayName("할당 상태 변경을 저장하면 반영된다")
    void updateAllocation() {
        StockAllocation saved = allocation(lineId, 11L, 5);
        saved.pick(3);

        allocationRepository.save(saved);

        StockAllocation found = allocationRepository.findByIdForUpdate(saved.getAllocationId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(AllocationStatus.PICKED);
        assertThat(found.getPickedQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("발주 단위 할당 조회는 해당 발주의 항목만 ID 오름차순으로 돌려준다")
    void findByStoreOrderId() {
        StockAllocation a1 = allocation(lineId, 11L, 5);
        StockAllocation a2 = allocation(lineId, 12L, 3);
        allocation(otherLineId, 13L, 2);

        List<StockAllocation> found = allocationRepository.findByStoreOrderId(orderId);

        assertThat(found).extracting(StockAllocation::getAllocationId)
                .containsExactly(a1.getAllocationId(), a2.getAllocationId());
    }

    @Test
    @DisplayName("출고에 연결되지 않은 ALLOCATED 할당만 묶을 대상이다(취소된 출고의 할당은 다시 대상)")
    void findUnlinkedAllocated() {
        StockAllocation free = allocation(lineId, 11L, 5);
        StockAllocation inReady = allocation(lineId, 12L, 5);
        StockAllocation inCanceled = allocation(lineId, 13L, 5);
        StockAllocation released = allocation(lineId, 14L, 5);
        released.release(NOW);
        allocationRepository.save(released);
        allocation(otherLineId, 15L, 5);

        outbound("OB-20261005-0001", orderId, inReady.getAllocationId());
        Outbound canceled = outbound("OB-20261005-0002", orderId, inCanceled.getAllocationId());
        canceled.cancel();
        outboundRepository.save(canceled);

        List<StockAllocation> targets = allocationRepository.findUnlinkedAllocatedByStoreOrderIdForUpdate(orderId);

        assertThat(targets).extracting(StockAllocation::getAllocationId)
                .containsExactly(free.getAllocationId(), inCanceled.getAllocationId());
    }

    @Test
    @DisplayName("취소되지 않은 출고에 연결된 할당인지 확인한다")
    void isLinkedToActiveOutbound() {
        StockAllocation linked = allocation(lineId, 11L, 5);
        StockAllocation inCanceled = allocation(lineId, 12L, 5);
        StockAllocation free = allocation(lineId, 13L, 5);
        outbound("OB-20261005-0001", orderId, linked.getAllocationId());
        Outbound canceled = outbound("OB-20261005-0002", orderId, inCanceled.getAllocationId());
        canceled.cancel();
        outboundRepository.save(canceled);

        assertThat(allocationRepository.isLinkedToActiveOutbound(linked.getAllocationId())).isTrue();
        assertThat(allocationRepository.isLinkedToActiveOutbound(inCanceled.getAllocationId())).isFalse();
        assertThat(allocationRepository.isLinkedToActiveOutbound(free.getAllocationId())).isFalse();
    }

    @Test
    @DisplayName("발주의 ALLOCATED 할당 존재 여부와 상태별 잠금 조회")
    void existsAndLockByStatus() {
        StockAllocation a = allocation(lineId, 11L, 5);

        assertThat(allocationRepository.existsByStoreOrderIdAndStatus(orderId, AllocationStatus.ALLOCATED)).isTrue();
        assertThat(allocationRepository.existsByStoreOrderIdAndStatus(otherOrderId, AllocationStatus.ALLOCATED)).isFalse();
        assertThat(allocationRepository.findByStoreOrderIdAndStatusForUpdate(orderId, AllocationStatus.ALLOCATED))
                .extracting(StockAllocation::getAllocationId).containsExactly(a.getAllocationId());
        assertThat(allocationRepository.findAllByIdForUpdate(Set.of(a.getAllocationId()))).hasSize(1);
        assertThat(allocationRepository.findAllByIdForUpdate(Set.of())).isEmpty();
    }

    @Test
    @DisplayName("출고와 항목을 저장하고 항목을 ID 오름차순으로 읽는다")
    void saveOutboundWithLines() {
        StockAllocation a1 = allocation(lineId, 11L, 5);
        StockAllocation a2 = allocation(lineId, 12L, 3);

        Outbound saved = outbound("OB-20261005-0001", orderId, a1.getAllocationId(), a2.getAllocationId());

        Outbound found = outboundRepository.findById(saved.getOutboundId()).orElseThrow();
        List<OutboundLine> lines = outboundRepository.findLinesByOutboundId(saved.getOutboundId());
        assertThat(found.getStatus()).isEqualTo(OutboundStatus.READY);
        assertThat(found.getOutboundNo()).isEqualTo("OB-20261005-0001");
        assertThat(lines).extracting(OutboundLine::getAllocationId)
                .containsExactly(a1.getAllocationId(), a2.getAllocationId());
        assertThat(lines.get(0).getShippedQuantity()).isZero();
        assertThat(lines.get(0).getConfirmedUnitSupplyPrice()).isNull();
    }

    @Test
    @DisplayName("피킹 확정을 저장하면 수량과 단가가 반영된다")
    void saveConfirmedLine() {
        StockAllocation a = allocation(lineId, 11L, 5);
        Outbound saved = outbound("OB-20261005-0001", orderId, a.getAllocationId());
        OutboundLine line = outboundRepository.findLinesByOutboundId(saved.getOutboundId()).get(0);
        line.confirmPicking(4, 5, new BigDecimal("1500.00"));

        outboundRepository.saveLines(List.of(line));

        OutboundLine found = outboundRepository.findLinesByOutboundId(saved.getOutboundId()).get(0);
        assertThat(found.getShippedQuantity()).isEqualTo(4);
        assertThat(found.getConfirmedUnitSupplyPrice()).isEqualByComparingTo("1500.00");
    }

    @Test
    @DisplayName("발주별 출고는 생성 순이고 상태 존재 여부를 확인한다")
    void outboundsByStoreOrder() {
        Outbound first = outbound("OB-20261005-0001", orderId);
        first.cancel();
        outboundRepository.save(first);
        Outbound second = outbound("OB-20261005-0002", orderId);
        second.startPicking();
        outboundRepository.save(second);
        outbound("OB-20261005-0003", otherOrderId);

        assertThat(outboundRepository.findByStoreOrderId(orderId)).extracting(Outbound::getOutboundNo)
                .containsExactly("OB-20261005-0001", "OB-20261005-0002");
        assertThat(outboundRepository.findByStoreOrderIds(Set.of(orderId, otherOrderId))).hasSize(3);
        assertThat(outboundRepository.findByStoreOrderIds(Set.of())).isEmpty();
        assertThat(outboundRepository.existsByStoreOrderIdAndStatusIn(orderId,
                Set.of(OutboundStatus.PICKING, OutboundStatus.PICKED))).isTrue();
        assertThat(outboundRepository.existsByStoreOrderIdAndStatusIn(otherOrderId,
                Set.of(OutboundStatus.PICKING, OutboundStatus.PICKED))).isFalse();
        assertThat(outboundRepository.existsNotCanceledByStoreOrderId(orderId)).isTrue();
        assertThat(outboundRepository.findByStoreOrderIdAndStatusForUpdate(otherOrderId, OutboundStatus.READY))
                .hasSize(1);
        assertThat(outboundRepository.findByIdForUpdate(second.getOutboundId())).isPresent();
    }

    @Test
    @DisplayName("취소된 출고뿐이면 취소되지 않은 출고가 없다")
    void onlyCanceled() {
        Outbound o = outbound("OB-20261005-0001", orderId);
        o.cancel();
        outboundRepository.save(o);

        assertThat(outboundRepository.existsNotCanceledByStoreOrderId(orderId)).isFalse();
    }

    @Test
    @DisplayName("출고 번호 중복 확인과 접두사별 채번 개수")
    void outboundNo() {
        outbound("OB-20261005-0001", orderId);
        outbound("OB-20261005-0002", orderId);
        outbound("OB-20261006-0001", orderId);

        assertThat(outboundRepository.existsByOutboundNo("OB-20261005-0001")).isTrue();
        assertThat(outboundRepository.existsByOutboundNo("OB-20261005-0009")).isFalse();
        assertThat(outboundRepository.countByOutboundNoPrefix("OB-20261005-")).isEqualTo(2);
    }
}
