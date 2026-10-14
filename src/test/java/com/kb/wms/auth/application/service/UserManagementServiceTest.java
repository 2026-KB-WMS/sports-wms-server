package com.kb.wms.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kb.wms.auth.application.port.in.UserUseCase;
import com.kb.wms.auth.application.port.in.command.UserUpdateCommand;
import com.kb.wms.auth.application.port.out.StoreOwnerAssignPort;
import com.kb.wms.auth.application.port.out.WarehouseManagerAssignPort;
import com.kb.wms.auth.domain.entity.User;
import com.kb.wms.auth.domain.enums.UserStatus;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.security.UserRole;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long USER_ID = 12L;

    @Mock
    private UserUseCase userUseCase;
    @Mock
    private WarehouseManagerAssignPort warehouseManagerAssignPort;
    @Mock
    private StoreOwnerAssignPort storeOwnerAssignPort;

    @InjectMocks
    private UserManagementService userManagementService;

    private User user(UserRole role) {
        return User.builder().userId(USER_ID).loginId("member01").passwordHash("hashed").name("김담당")
                .email("member01@example.com").phone("010-1234-5678").role(role).status(UserStatus.ACTIVE).build();
    }

    private UserUpdateCommand command(Long warehouseId, Long storeId) {
        return new UserUpdateCommand(USER_ID, ADMIN_ID, null, null, null, null, null, null, UserStatus.ACTIVE,
                warehouseId, storeId);
    }

    @Test
    @DisplayName("창고 관리자 승인은 상태를 바꾼 뒤 MANAGER 역할로 창고에 배정한다")
    void updateUser_assignsWarehouseAfterStatusChange() {
        UserUpdateCommand command = command(3L, null);
        when(userUseCase.updateUser(command)).thenReturn(user(UserRole.WAREHOUSE_MANAGER));

        User result = userManagementService.updateUser(command);

        assertThat(result.getUserId()).isEqualTo(USER_ID);
        InOrder order = inOrder(userUseCase, warehouseManagerAssignPort);
        order.verify(userUseCase).updateUser(command);
        order.verify(warehouseManagerAssignPort).assignManager(3L, USER_ID);
        verifyNoInteractions(storeOwnerAssignPort);
    }

    @Test
    @DisplayName("점주 승인은 상태를 바꾼 뒤 OWNER 역할로 지점에 배정한다")
    void updateUser_assignsStoreAfterStatusChange() {
        UserUpdateCommand command = command(null, 5L);
        when(userUseCase.updateUser(command)).thenReturn(user(UserRole.STORE_OWNER));

        userManagementService.updateUser(command);

        InOrder order = inOrder(userUseCase, storeOwnerAssignPort);
        order.verify(userUseCase).updateUser(command);
        order.verify(storeOwnerAssignPort).assignOwner(5L, USER_ID);
        verifyNoInteractions(warehouseManagerAssignPort);
    }

    @Test
    @DisplayName("소속 필드가 없으면 배정하지 않고 수정만 처리한다")
    void updateUser_noAffiliationFields() {
        UserUpdateCommand command = command(null, null);
        when(userUseCase.updateUser(command)).thenReturn(user(UserRole.WAREHOUSE_MANAGER));

        userManagementService.updateUser(command);

        verifyNoInteractions(warehouseManagerAssignPort, storeOwnerAssignPort);
    }

    @Test
    @DisplayName("배정이 실패하면 예외를 그대로 던져 같은 트랜잭션의 승인도 되돌린다")
    void updateUser_assignFailurePropagates() {
        UserUpdateCommand command = command(3L, null);
        when(userUseCase.updateUser(command)).thenReturn(user(UserRole.WAREHOUSE_MANAGER));
        doThrow(new BusinessException(com.kb.wms.common.exception.ErrorCode.CONFLICT, "비활성 창고"))
                .when(warehouseManagerAssignPort).assignManager(any(), any());

        assertThatThrownBy(() -> userManagementService.updateUser(command)).isInstanceOf(BusinessException.class);
        verify(storeOwnerAssignPort, never()).assignOwner(any(), any());
    }
}
