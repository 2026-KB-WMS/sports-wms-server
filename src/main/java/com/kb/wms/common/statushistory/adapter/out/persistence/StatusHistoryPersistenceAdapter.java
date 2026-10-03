package com.kb.wms.common.statushistory.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kb.wms.common.statushistory.adapter.out.persistence.entity.StatusHistoryJpaEntity;
import com.kb.wms.common.statushistory.adapter.out.persistence.repository.StatusHistoryJpaRepository;
import com.kb.wms.common.statushistory.application.port.out.StatusHistoryRepository;
import com.kb.wms.common.statushistory.domain.entity.StatusHistory;
import com.kb.wms.common.statushistory.domain.enums.StatusHistoryEntityType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StatusHistoryPersistenceAdapter implements StatusHistoryRepository {

    private final StatusHistoryJpaRepository statusHistoryJpaRepository;

    @Override
    public StatusHistory save(StatusHistory statusHistory) {
        return statusHistoryJpaRepository.save(StatusHistoryJpaEntity.fromDomain(statusHistory)).toDomain();
    }

    @Override
    public List<StatusHistory> findByEntity(StatusHistoryEntityType entityType, Long entityId) {
        return statusHistoryJpaRepository
                .findByEntityTypeAndEntityIdOrderByChangedAtAscStatusHistoryIdAsc(entityType, entityId)
                .stream()
                .map(StatusHistoryJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<StatusHistory> findLatestByToStatus(StatusHistoryEntityType entityType, Long entityId,
                                                        String toStatus) {
        return statusHistoryJpaRepository
                .findFirstByEntityTypeAndEntityIdAndToStatusOrderByChangedAtDescStatusHistoryIdDesc(
                        entityType, entityId, toStatus)
                .map(StatusHistoryJpaEntity::toDomain);
    }
}
