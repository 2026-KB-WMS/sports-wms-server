package com.kb.wms.auth.application.port.in.command;

import com.kb.wms.auth.domain.enums.UserStatus;
import com.kb.wms.common.security.UserRole;

/**
 * PATCH /api/v1/users/{userId} 요청. 모든 필드는 선택이며, 값이 있는 필드만 부분 수정한다.
 * loginId·password는 수정할 수 없지만 요청에 담겨 오면 무시하지 않고 거절해야 하므로 서비스가 검사할 수 있게 둔다.
 *
 * @param actorUserId 요청한 관리자 ID. 본인 계정 보호와 상태 이력의 변경자 기록에 쓴다.
 * @param warehouseId 승인·재활성화와 함께 배정할 창고(창고 관리자). storeId와 함께 보낼 수 없다.
 * @param storeId     승인·재활성화와 함께 배정할 지점(점주). warehouseId와 함께 보낼 수 없다.
 */
public record UserUpdateCommand(
        Long userId,
        Long actorUserId,
        String loginId,
        String password,
        String name,
        String email,
        String phone,
        UserRole role,
        UserStatus status,
        Long warehouseId,
        Long storeId
) {

    public UserUpdateCommand(Long userId, Long actorUserId, String loginId, String password, String name,
                             String email, String phone, UserRole role, UserStatus status) {
        this(userId, actorUserId, loginId, password, name, email, phone, role, status, null, null);
    }

    public boolean hasNoChanges() {
        return name == null && email == null && phone == null && role == null && status == null
                && warehouseId == null && storeId == null;
    }

    public boolean assignsAffiliation() {
        return warehouseId != null || storeId != null;
    }
}
