package com.kb.wms.outbound.adapter.out.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kb.wms.outbound.adapter.out.persistence.entity.OutboundJpaEntity;
import com.kb.wms.outbound.domain.enums.OutboundStatus;

import jakarta.persistence.LockModeType;

public interface OutboundJpaRepository extends JpaRepository<OutboundJpaEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OutboundJpaEntity o where o.outboundId = :outboundId")
    Optional<OutboundJpaEntity> findByIdForUpdate(@Param("outboundId") Long outboundId);

    boolean existsByOutboundNo(String outboundNo);

    long countByOutboundNoStartingWith(String prefix);

    /** 생성 순(출고 ID 오름차순) */
    List<OutboundJpaEntity> findByStoreOrderIdOrderByOutboundIdAsc(Long storeOrderId);

    List<OutboundJpaEntity> findByStoreOrderIdInOrderByOutboundIdAsc(Collection<Long> storeOrderIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select o from OutboundJpaEntity o
            where o.storeOrderId = :storeOrderId and o.status = :status
            order by o.outboundId asc
            """)
    List<OutboundJpaEntity> findByStoreOrderIdAndStatusForUpdate(
            @Param("storeOrderId") Long storeOrderId, @Param("status") OutboundStatus status);

    boolean existsByStoreOrderIdAndStatusIn(Long storeOrderId, Collection<OutboundStatus> statuses);

    boolean existsByStoreOrderIdAndStatusNot(Long storeOrderId, OutboundStatus status);
}
