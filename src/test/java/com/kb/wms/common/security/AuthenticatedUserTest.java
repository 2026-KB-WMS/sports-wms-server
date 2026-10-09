package com.kb.wms.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;

class AuthenticatedUserTest {

    private final AuthenticatedUser hqAdmin = new AuthenticatedUser(1L, UserRole.HQ_ADMIN, List.of(), List.of());
    private final AuthenticatedUser warehouseManager =
            new AuthenticatedUser(2L, UserRole.WAREHOUSE_MANAGER, List.of(10L, 11L), List.of());
    private final AuthenticatedUser storeOwner =
            new AuthenticatedUser(3L, UserRole.STORE_OWNER, List.of(), List.of(20L));

    @Test
    @DisplayName("본사 관리자는 소속 배정 없이도 모든 창고·지점에 접근할 수 있다")
    void hqAdminCanAccessEverything() {
        assertThat(hqAdmin.isHqAdmin()).isTrue();
        assertThat(hqAdmin.canAccessWarehouse(999L)).isTrue();
        assertThat(hqAdmin.canAccessStore(999L)).isTrue();
    }

    @Test
    @DisplayName("창고 관리자는 배정된 창고에만 접근하고 지점에는 접근할 수 없다")
    void warehouseManagerScope() {
        assertThat(warehouseManager.isHqAdmin()).isFalse();
        assertThat(warehouseManager.canAccessWarehouse(10L)).isTrue();
        assertThat(warehouseManager.canAccessWarehouse(12L)).isFalse();
        assertThat(warehouseManager.canAccessStore(20L)).isFalse();
    }

    @Test
    @DisplayName("점주는 배정된 지점에만 접근하고 창고에는 접근할 수 없다")
    void storeOwnerScope() {
        assertThat(storeOwner.canAccessStore(20L)).isTrue();
        assertThat(storeOwner.canAccessStore(21L)).isFalse();
        assertThat(storeOwner.canAccessWarehouse(10L)).isFalse();
    }

    @Test
    @DisplayName("범위 밖 창고·지점을 require하면 403 FORBIDDEN, 범위 안이면 통과한다")
    void requireThrowsForbiddenOutsideScope() {
        assertThatCode(() -> warehouseManager.requireWarehouseAccess(11L)).doesNotThrowAnyException();
        assertThatCode(() -> storeOwner.requireStoreAccess(20L)).doesNotThrowAnyException();

        assertThatThrownBy(() -> warehouseManager.requireWarehouseAccess(12L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
        assertThatThrownBy(() -> storeOwner.requireStoreAccess(21L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
    }
}
