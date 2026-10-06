package com.kb.wms.common.security;

import java.util.List;

import com.kb.wms.auth.domain.enums.UserRole;

/**
 * 액세스 토큰 클레임에서 복원한 인증 주체. 요청 처리 중 SecurityContext의 principal로 쓰인다.
 * 토큰 발급 시점의 값이므로 소속·역할이 이후에 바뀌어도 토큰이 만료될 때까지 그대로 유지된다.
 *
 * @param warehouseIds 배정된 창고 ID
 * @param storeIds     배정된 지점 ID
 */
public record AuthenticatedUser(
        Long userId,
        UserRole role,
        List<Long> warehouseIds,
        List<Long> storeIds
) {

    public AuthenticatedUser {
        warehouseIds = List.copyOf(warehouseIds);
        storeIds = List.copyOf(storeIds);
    }
}
