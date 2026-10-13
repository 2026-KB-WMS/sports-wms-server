package com.kb.wms.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.kb.wms.auth.application.port.in.UserManagementUseCase;
import com.kb.wms.auth.application.port.in.UserUseCase;
import com.kb.wms.auth.application.port.in.command.UserSignupCommand;
import com.kb.wms.auth.application.port.in.command.UserUpdateCommand;
import com.kb.wms.auth.domain.entity.User;
import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.auth.domain.enums.UserStatus;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.store.application.port.in.StoreMemberUseCase;
import com.kb.wms.store.application.port.in.StoreUseCase;
import com.kb.wms.store.application.port.in.command.StoreRegisterCommand;
import com.kb.wms.warehouse.application.port.in.WarehouseMemberUseCase;
import com.kb.wms.warehouse.application.port.in.WarehouseUseCase;
import com.kb.wms.warehouse.application.port.in.command.WarehouseRegisterCommand;

/**
 * 승인과 소속 배정이 한 트랜잭션인지 실제 빈 연결로 확인한다. 배정이 실패하면 승인(상태 변경)도 되돌려져야 하므로
 * 테스트 트랜잭션으로 감싸지 않고 커밋·롤백 결과를 그대로 본다.
 */
@SpringBootTest
@TestPropertySource(properties = "spring.flyway.enabled=false")
class UserApprovalWithAffiliationTest {

    private static final Long ADMIN_ID = 9_999L;
    private static final String PASSWORD = "P@ssw0rd!";

    @Autowired UserUseCase userUseCase;
    @Autowired UserManagementUseCase userManagementUseCase;
    @Autowired WarehouseUseCase warehouseUseCase;
    @Autowired WarehouseMemberUseCase warehouseMemberUseCase;
    @Autowired StoreUseCase storeUseCase;
    @Autowired StoreMemberUseCase storeMemberUseCase;

    private User signUp(String loginId, UserRole role) {
        return userUseCase.signUp(new UserSignupCommand(loginId, PASSWORD, "이름", loginId + "@example.com",
                "010-1234-5678", role));
    }

    private UserUpdateCommand approve(Long userId, Long warehouseId, Long storeId) {
        return new UserUpdateCommand(userId, ADMIN_ID, null, null, null, null, null, null, UserStatus.ACTIVE,
                warehouseId, storeId);
    }

    @Test
    @DisplayName("창고 관리자를 소속과 함께 승인하면 ACTIVE가 되고 창고 배정이 생긴다")
    void approveWarehouseManagerWithWarehouse() {
        Long warehouseId = warehouseUseCase.registerWarehouse(new WarehouseRegisterCommand(
                "승인용 창고", "서울 강남구 테헤란로 1", "02-123-4567", BigDecimal.TEN)).getWarehouseId();
        User user = signUp("appr_wm_ok", UserRole.WAREHOUSE_MANAGER);

        User result = userManagementUseCase.updateUser(approve(user.getUserId(), warehouseId, null));

        assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(warehouseMemberUseCase.getManagers(warehouseId, user.getUserId(), null)).hasSize(1);
    }

    @Test
    @DisplayName("점주를 소속과 함께 승인하면 ACTIVE가 되고 지점 배정이 생긴다")
    void approveStoreOwnerWithStore() {
        Long storeId = storeUseCase.registerStore(new StoreRegisterCommand(
                "승인용 지점", "경기 성남시 분당구 판교로 1", "점주", "031-123-4567")).getStoreId();
        User user = signUp("appr_so_ok", UserRole.STORE_OWNER);

        User result = userManagementUseCase.updateUser(approve(user.getUserId(), null, storeId));

        assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(storeMemberUseCase.getManagers(storeId, user.getUserId(), null)).hasSize(1);
    }

    @Test
    @DisplayName("없는 창고를 지정하면 배정이 실패하고 승인도 되돌려져 PENDING으로 남는다")
    void assignFailureRollsBackApproval() {
        User user = signUp("appr_wm_fail", UserRole.WAREHOUSE_MANAGER);

        assertThatThrownBy(() -> userManagementUseCase.updateUser(approve(user.getUserId(), 987_654L, null)))
                .isInstanceOf(BusinessException.class);

        assertThat(userUseCase.getUser(user.getUserId()).getStatus()).isEqualTo(UserStatus.PENDING);
    }

    @Test
    @DisplayName("승인 대기 사용자에게 배정 API를 직접 쓰면 거절되고, 소속 없는 승인은 AFFILIATION_REQUIRED다")
    void pendingUserCannotBeAssignedFirst() {
        Long warehouseId = warehouseUseCase.registerWarehouse(new WarehouseRegisterCommand(
                "배정 거절 창고", "부산 해운대구 센텀로 1", "051-123-4567", BigDecimal.TEN)).getWarehouseId();
        User user = signUp("appr_wm_pending", UserRole.WAREHOUSE_MANAGER);

        assertThatThrownBy(() -> warehouseMemberUseCase.assignManager(
                new com.kb.wms.warehouse.application.port.in.command.WarehouseMemberAssignCommand(
                        warehouseId, user.getUserId(), "MANAGER")))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> userManagementUseCase.updateUser(approve(user.getUserId(), null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo("AFFILIATION_REQUIRED");
    }
}
