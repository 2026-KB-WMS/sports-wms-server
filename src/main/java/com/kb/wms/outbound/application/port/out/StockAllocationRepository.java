package com.kb.wms.outbound.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.kb.wms.outbound.domain.entity.StockAllocation;
import com.kb.wms.outbound.domain.enums.AllocationStatus;

/**
 * 재고 할당 영속성 아웃바운드 포트. 발주는 ID로만 참조하므로 발주 단위 조회는 발주 항목 ID를 거쳐 읽기 전용으로 한다(ADR-005, ADR-007).
 */
public interface StockAllocationRepository {

    /** 새 할당을 저장하거나 ID가 있으면 변경(상태·피킹 수량·해제 일시)을 반영한다. */
    StockAllocation save(StockAllocation allocation);

    List<StockAllocation> saveAll(List<StockAllocation> allocations);

    Optional<StockAllocation> findById(Long allocationId);

    /** 비관적 쓰기 락으로 할당을 조회한다. 해제처럼 상태를 확인하고 바꾸는 흐름에서 쓴다. */
    Optional<StockAllocation> findByIdForUpdate(Long allocationId);

    /** 할당 ID 오름차순으로 비관적 쓰기 락을 건다. 피킹 완료·출고 항목 처리에서 쓴다. */
    List<StockAllocation> findAllByIdForUpdate(Collection<Long> allocationIds);

    /** 발주에 딸린 할당(상태 무관), 할당 ID 오름차순 */
    List<StockAllocation> findByStoreOrderId(Long storeOrderId);

    /** 발주에 딸린 해당 상태의 할당을 잠그고 조회한다(할당 ID 오름차순). 발주 취소에 따른 일괄 해제에서 쓴다. */
    List<StockAllocation> findByStoreOrderIdAndStatusForUpdate(Long storeOrderId, AllocationStatus status);

    /** 취소되지 않은 출고에 연결되지 않은 ALLOCATED 할당을 잠그고 조회한다. 출고 생성 때 묶을 대상이다. */
    List<StockAllocation> findUnlinkedAllocatedByStoreOrderIdForUpdate(Long storeOrderId);

    boolean existsByStoreOrderIdAndStatus(Long storeOrderId, AllocationStatus status);

    /** 취소되지 않은 출고 항목에 연결된 할당인지. */
    boolean isLinkedToActiveOutbound(Long allocationId);
}
