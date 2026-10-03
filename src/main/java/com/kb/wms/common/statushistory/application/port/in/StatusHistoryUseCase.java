package com.kb.wms.common.statushistory.application.port.in;

import java.util.List;
import java.util.Optional;

import com.kb.wms.common.statushistory.domain.entity.StatusHistory;
import com.kb.wms.common.statushistory.domain.enums.StatusHistoryEntityType;

/**
 * 상태 이력 인바운드 포트. 다른 도메인 서비스가 상태를 바꿀 때 호출한다.
 */
public interface StatusHistoryUseCase {

    /**
     * 상태 변경을 기록한다. 호출한 도메인 서비스의 트랜잭션에 반드시 참여하므로
     * 호출 쪽이 롤백되면 이력도 함께 롤백된다. 진행 중인 트랜잭션이 없으면 예외다.
     * 최초 생성이면 fromStatus에 null을 넣는다.
     */
    StatusHistory record(StatusHistoryEntityType entityType, Long entityId,
                         String fromStatus, String toStatus, String reason, Long changedBy);

    /** 엔티티별 이력을 시간순(오래된 것부터)으로 조회한다. */
    List<StatusHistory> findHistory(StatusHistoryEntityType entityType, Long entityId);

    /** 현재 상태로 바뀔 때 기록된 사유(statusReason). 이력이나 사유가 없으면 비어 있다. */
    Optional<String> findStatusReason(StatusHistoryEntityType entityType, Long entityId, String currentStatus);
}
