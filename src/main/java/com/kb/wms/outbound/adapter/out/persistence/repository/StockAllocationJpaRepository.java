package com.kb.wms.outbound.adapter.out.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kb.wms.outbound.adapter.out.persistence.entity.StockAllocationJpaEntity;
import com.kb.wms.outbound.domain.enums.AllocationStatus;

import jakarta.persistence.LockModeType;

public interface StockAllocationJpaRepository extends JpaRepository<StockAllocationJpaEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from StockAllocationJpaEntity a where a.allocationId = :allocationId")
    Optional<StockAllocationJpaEntity> findByIdForUpdate(@Param("allocationId") Long allocationId);

    /** 할당 ID 오름차순으로 잠근다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select a from StockAllocationJpaEntity a
            where a.allocationId in :allocationIds
            order by a.allocationId asc
            """)
    List<StockAllocationJpaEntity> findAllByIdForUpdate(@Param("allocationIds") Collection<Long> allocationIds);

    /** 발주 항목(ID 참조)을 거쳐 발주의 할당을 조회한다. 할당 ID 오름차순. */
    @Query("""
            select a from StockAllocationJpaEntity a
            where a.storeOrderLineId in (
                select l.storeOrderLineId from StoreOrderLineJpaEntity l where l.storeOrderId = :storeOrderId)
            order by a.allocationId asc
            """)
    List<StockAllocationJpaEntity> findByStoreOrderId(@Param("storeOrderId") Long storeOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select a from StockAllocationJpaEntity a
            where a.status = :status
              and a.storeOrderLineId in (
                select l.storeOrderLineId from StoreOrderLineJpaEntity l where l.storeOrderId = :storeOrderId)
            order by a.allocationId asc
            """)
    List<StockAllocationJpaEntity> findByStoreOrderIdAndStatusForUpdate(
            @Param("storeOrderId") Long storeOrderId, @Param("status") AllocationStatus status);

    /** 취소되지 않은 출고에 연결되지 않은 ALLOCATED 할당. 출고 생성 때 묶을 대상이다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select a from StockAllocationJpaEntity a
            where a.status = com.kb.wms.outbound.domain.enums.AllocationStatus.ALLOCATED
              and a.storeOrderLineId in (
                select l.storeOrderLineId from StoreOrderLineJpaEntity l where l.storeOrderId = :storeOrderId)
              and not exists (
                select 1 from OutboundLineJpaEntity ol, OutboundJpaEntity o
                where ol.allocationId = a.allocationId
                  and o.outboundId = ol.outboundId
                  and o.status <> com.kb.wms.outbound.domain.enums.OutboundStatus.CANCELED)
            order by a.allocationId asc
            """)
    List<StockAllocationJpaEntity> findUnlinkedAllocatedByStoreOrderIdForUpdate(
            @Param("storeOrderId") Long storeOrderId);

    @Query("""
            select count(a) > 0 from StockAllocationJpaEntity a
            where a.status = :status
              and a.storeOrderLineId in (
                select l.storeOrderLineId from StoreOrderLineJpaEntity l where l.storeOrderId = :storeOrderId)
            """)
    boolean existsByStoreOrderIdAndStatus(@Param("storeOrderId") Long storeOrderId,
                                          @Param("status") AllocationStatus status);
}
