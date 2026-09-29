package com.kb.wms.store.application.port.in;

import java.util.List;

import com.kb.wms.store.application.port.in.command.StoreMemberAssignCommand;
import com.kb.wms.store.domain.entity.StoreMember;

/**
 * 지점 관리자(점주) 배정/조회/배정해제 유스케이스.
 * POST /api/v1/stores/assign, GET /api/v1/stores/managers, DELETE /api/v1/stores/managers/{storeMemberId}
 */
public interface StoreMemberUseCase {

    StoreMember assignManager(StoreMemberAssignCommand command);

    /**
     * storeId·userId가 null이면 해당 조건을 무시하고 전체 배정을 조회한다.
     */
    List<StoreMember> getManagers(Long storeId, Long userId);

    void releaseManager(Long storeMemberId);
}
