package com.kb.wms.statushistory.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kb.wms.statushistory.adapter.out.persistence.entity.StatusHistoryJpaEntity;
import com.kb.wms.statushistory.adapter.out.persistence.repository.StatusHistoryJpaRepository;
import com.kb.wms.statushistory.application.port.in.result.StatusHistoryView;
import com.kb.wms.statushistory.application.port.out.StatusHistoryRepository;
import com.kb.wms.statushistory.domain.entity.StatusHistory;
import com.kb.wms.statushistory.domain.enums.StatusHistoryEntityType;

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
    public List<StatusHistoryView> findViewsByEntity(StatusHistoryEntityType entityType, Long entityId) {
        return statusHistoryJpaRepository.findViewsByEntity(entityType, entityId);
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
