package com.kb.wms.statushistory.adapter.out.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kb.wms.statushistory.adapter.out.persistence.entity.StatusHistoryJpaEntity;
import com.kb.wms.statushistory.domain.enums.StatusHistoryEntityType;

public interface StatusHistoryJpaRepository extends JpaRepository<StatusHistoryJpaEntity, Long> {

    // changed_at이 같은 이력(같은 트랜잭션에서 연속 기록)은 PK로 순서를 고정한다.
    List<StatusHistoryJpaEntity> findByEntityTypeAndEntityIdOrderByChangedAtAscStatusHistoryIdAsc(
            StatusHistoryEntityType entityType, Long entityId);

    Optional<StatusHistoryJpaEntity> findFirstByEntityTypeAndEntityIdAndToStatusOrderByChangedAtDescStatusHistoryIdDesc(
            StatusHistoryEntityType entityType, Long entityId, String toStatus);
}
