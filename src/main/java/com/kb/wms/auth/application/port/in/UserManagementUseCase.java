package com.kb.wms.auth.application.port.in;

import com.kb.wms.auth.application.port.in.command.UserUpdateCommand;
import com.kb.wms.auth.domain.entity.User;

/**
 * 계정 수정과 소속 배정을 한 트랜잭션으로 묶는 유스케이스. PATCH /api/v1/users/{userId}
 */
public interface UserManagementUseCase {

    /**
     * 부분 수정. warehouseId·storeId가 함께 오면 승인(재활성화)과 같은 트랜잭션에서 소속을 배정하며,
     * 배정이 실패하면 상태 변경도 반영되지 않는다. 소속이 없는 창고 관리자·점주를 소속 지정 없이 승인하면
     * 409 AFFILIATION_REQUIRED, 이미 소속이 있는 사용자에게 소속을 보내면 409 AFFILIATION_ASSIGNED다.
     */
    User updateUser(UserUpdateCommand command);
}
