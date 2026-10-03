package com.kb.wms.common.statushistory.application.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.statushistory.application.port.in.StatusHistoryUseCase;
import com.kb.wms.common.statushistory.application.port.out.StatusHistoryRepository;
import com.kb.wms.common.statushistory.domain.entity.StatusHistory;
import com.kb.wms.common.statushistory.domain.enums.StatusHistoryEntityType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatusHistoryService implements StatusHistoryUseCase {

    private final StatusHistoryRepository statusHistoryRepository;

    // MANDATORY: 호출한 도메인 서비스 트랜잭션에 참여한다. 없으면 예외로 단독 커밋을 막는다.
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public StatusHistory record(StatusHistoryEntityType entityType, Long entityId,
                                String fromStatus, String toStatus, String reason, Long changedBy) {
        StatusHistory history = StatusHistory.record(
                entityType, entityId, fromStatus, toStatus, reason, changedBy, LocalDateTime.now());
        return statusHistoryRepository.save(history);
    }

    @Override
    public List<StatusHistory> findHistory(StatusHistoryEntityType entityType, Long entityId) {
        return statusHistoryRepository.findByEntity(entityType, entityId);
    }

    @Override
    public Optional<String> findStatusReason(StatusHistoryEntityType entityType, Long entityId,
                                             String currentStatus) {
        return statusHistoryRepository.findLatestByToStatus(entityType, entityId, currentStatus)
                .map(StatusHistory::getReason);
    }
}
