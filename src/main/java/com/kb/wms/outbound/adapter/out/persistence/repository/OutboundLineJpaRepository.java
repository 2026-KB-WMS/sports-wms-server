package com.kb.wms.outbound.adapter.out.persistence.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kb.wms.outbound.adapter.out.persistence.entity.OutboundLineJpaEntity;

public interface OutboundLineJpaRepository extends JpaRepository<OutboundLineJpaEntity, Long> {

    /** 출고 항목 ID 오름차순 */
    List<OutboundLineJpaEntity> findByOutboundIdOrderByOutboundLineIdAsc(Long outboundId);

    List<OutboundLineJpaEntity> findByOutboundIdIn(Collection<Long> outboundIds);

    /** 취소되지 않은 출고에 연결된 항목이 있는지. 할당 해제 가드(ALLOCATION_IN_OUTBOUND)에 쓴다. */
    @Query("""
            select count(l) > 0 from OutboundLineJpaEntity l, OutboundJpaEntity o
            where l.allocationId = :allocationId
              and o.outboundId = l.outboundId
              and o.status <> com.kb.wms.outbound.domain.enums.OutboundStatus.CANCELED
            """)
    boolean existsLinkedToActiveOutbound(@Param("allocationId") Long allocationId);
}
