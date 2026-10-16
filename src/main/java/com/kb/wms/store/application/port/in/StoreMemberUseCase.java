package com.kb.wms.store.application.port.in;

import com.kb.wms.common.paging.PageCondition;
import com.kb.wms.common.paging.PageResult;
import com.kb.wms.store.application.port.in.command.StoreMemberAssignCommand;
import com.kb.wms.store.application.port.in.result.StoreMemberView;

/**
 * 지점 관리자(점주) 배정/조회/배정해제 유스케이스.
 * POST /api/v1/stores/assign, GET /api/v1/stores/managers, DELETE /api/v1/stores/managers/{storeMemberId}
 */
public interface StoreMemberUseCase {

    /**
     * 대상 사용자가 없으면 404 USER_NOT_FOUND, STORE_OWNER가 아니면 400, ACTIVE가 아닌 사용자(PENDING·INACTIVE)면 409.
     * 승인 전 사용자의 소속은 승인 요청(PATCH /users/{userId})에서 함께 지정한다.
     */
    StoreMemberView assignManager(StoreMemberAssignCommand command);

    /**
     * storeId·userId·keyword(사용자 이름·로그인 아이디 부분 일치)가 null이면 해당 조건을 무시하고 조회한다.
     */
    PageResult<StoreMemberView> getManagers(Long storeId, Long userId, String keyword, PageCondition page);

    void releaseManager(Long storeMemberId);
}
