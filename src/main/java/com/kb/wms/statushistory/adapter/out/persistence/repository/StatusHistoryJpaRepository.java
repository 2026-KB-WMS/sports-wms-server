package com.kb.wms.statushistory.adapter.out.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kb.wms.statushistory.adapter.out.persistence.entity.StatusHistoryJpaEntity;
import com.kb.wms.statushistory.application.port.in.result.StatusHistoryView;
import com.kb.wms.statushistory.domain.enums.StatusHistoryEntityType;

public interface StatusHistoryJpaRepository extends JpaRepository<StatusHistoryJpaEntity, Long> {

    // changed_at이 같은 이력(같은 트랜잭션에서 연속 기록)은 PK로 순서를 고정한다.
    List<StatusHistoryJpaEntity> findByEntityTypeAndEntityIdOrderByChangedAtAscStatusHistoryIdAsc(
            StatusHistoryEntityType entityType, Long entityId);

    Optional<StatusHistoryJpaEntity> findFirstByEntityTypeAndEntityIdAndToStatusOrderByChangedAtDescStatusHistoryIdDesc(
            StatusHistoryEntityType entityType, Long entityId, String toStatus);

    // 조회 전용: 처리자 이름만 필요하므로 사용자가 없어도 이력 행이 사라지지 않게 left join (ADR-007).
    @Query("""
            select new com.kb.wms.statushistory.application.port.in.result.StatusHistoryView(
                h.fromStatus, h.toStatus, h.reason, h.changedBy, u.name, h.changedAt)
            from StatusHistoryJpaEntity h
            left join UserJpaEntity u on u.userId = h.changedBy
            where h.entityType = :entityType and h.entityId = :entityId
            order by h.changedAt asc, h.statusHistoryId asc
            """)
    List<StatusHistoryView> findViewsByEntity(@Param("entityType") StatusHistoryEntityType entityType,
                                              @Param("entityId") Long entityId);
}
