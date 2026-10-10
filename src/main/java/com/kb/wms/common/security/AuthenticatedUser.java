package com.kb.wms.common.security;

import java.util.List;

import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;

/**
 * 인증 필터가 토큰의 사용자 ID로 DB에서 읽어 만든 인증 주체. 요청 처리 중 SecurityContext의 principal로 쓰이며,
 * 컨트롤러에서 {@code @AuthenticationPrincipal}로 받아 서비스에 넘기는 "현재 사용자 컨텍스트"다.
 * 요청마다 새로 만들므로 소속·역할 변경이 다음 요청부터 반영된다(ADR-016).
 * 소속 범위 검사({@code requireWarehouseAccess}, {@code requireStoreAccess})는 서비스가 대상 리소스를 읽은 뒤 호출한다.
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

    public boolean isHqAdmin() {
        return role == UserRole.HQ_ADMIN;
    }

    public boolean isWarehouseManager() {
        return role == UserRole.WAREHOUSE_MANAGER;
    }

    public boolean isStoreOwner() {
        return role == UserRole.STORE_OWNER;
    }

    /** 본사 관리자는 모든 창고, 그 외에는 배정된 창고만 접근할 수 있다. */
    public boolean canAccessWarehouse(Long warehouseId) {
        return isHqAdmin() || (warehouseId != null && warehouseIds.contains(warehouseId));
    }

    /** 본사 관리자는 모든 지점, 그 외에는 배정된 지점만 접근할 수 있다. */
    public boolean canAccessStore(Long storeId) {
        return isHqAdmin() || (storeId != null && storeIds.contains(storeId));
    }

    /**
     * 목록 조회에 적용할 창고 범위. 창고를 지정했으면 접근 권한을 확인하고(없으면 403) 추가 제한 없이 null,
     * 지정하지 않았으면 본사는 null(전체), 그 외에는 담당 창고 ID 목록(비어 있을 수 있음)이다. null이면 제한 없음.
     */
    public List<Long> warehouseScope(Long requestedWarehouseId) {
        if (requestedWarehouseId != null) {
            requireWarehouseAccess(requestedWarehouseId);
            return null;
        }
        return isHqAdmin() ? null : warehouseIds;
    }

    /** 창고 접근 권한이 없으면 403 FORBIDDEN. */
    public void requireWarehouseAccess(Long warehouseId) {
        if (!canAccessWarehouse(warehouseId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    /** 지점 접근 권한이 없으면 403 FORBIDDEN. */
    public void requireStoreAccess(Long storeId) {
        if (!canAccessStore(storeId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
