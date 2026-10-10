package com.kb.wms.statushistory.application.port.out;

import java.util.List;
import java.util.Optional;

import com.kb.wms.statushistory.application.port.in.result.StatusHistoryView;
import com.kb.wms.statushistory.domain.entity.StatusHistory;
import com.kb.wms.statushistory.domain.enums.StatusHistoryEntityType;

/**
 * 상태 이력 영속성 아웃바운드 포트.
 */
public interface StatusHistoryRepository {

    StatusHistory save(StatusHistory statusHistory);

    /** 엔티티 한 건의 이력을 시간순(오래된 것부터)으로 조회한다. */
    List<StatusHistory> findByEntity(StatusHistoryEntityType entityType, Long entityId);

    /** 엔티티 한 건의 이력을 처리자 이름과 함께 시간순(오래된 것부터)으로 조회한다. 조회 전용. */
    List<StatusHistoryView> findViewsByEntity(StatusHistoryEntityType entityType, Long entityId);

    /** 해당 상태로 가장 최근에 바뀐 이력. 한 번도 그 상태가 된 적이 없으면 비어 있다. */
    Optional<StatusHistory> findLatestByToStatus(StatusHistoryEntityType entityType, Long entityId, String toStatus);
}
