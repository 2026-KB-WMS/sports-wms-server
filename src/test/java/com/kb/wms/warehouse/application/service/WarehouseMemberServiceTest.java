package com.kb.wms.warehouse.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kb.wms.auth.application.port.in.UserUseCase;
import com.kb.wms.auth.domain.entity.User;
import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.auth.domain.enums.UserStatus;
import com.kb.wms.auth.exception.AuthErrorCode;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.warehouse.application.port.in.command.WarehouseMemberAssignCommand;
import com.kb.wms.warehouse.application.port.in.result.WarehouseMemberView;
import com.kb.wms.warehouse.application.port.out.WarehouseMemberRepository;
import com.kb.wms.warehouse.application.port.out.WarehouseRepository;
import com.kb.wms.warehouse.domain.entity.Warehouse;
import com.kb.wms.warehouse.domain.entity.WarehouseMember;
import com.kb.wms.warehouse.exception.WarehouseErrorCode;

@ExtendWith(MockitoExtension.class)
class WarehouseMemberServiceTest {

    @Mock
    private WarehouseMemberRepository warehouseMemberRepository;
    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private UserUseCase userUseCase;

    @InjectMocks
    private WarehouseMemberService warehouseMemberService;

    private final Warehouse activeWarehouse =
            Warehouse.register("WH-001", "서울 물류센터", "서울시 강남구", "02-1234-5678", BigDecimal.valueOf(1000));

    private User user(UserRole role, UserStatus status) {
        return User.builder().userId(10L).loginId("member01").passwordHash("hashed")
                .name("김담당").email("member01@example.com").phone("010-1234-5678")
                .role(role).status(status).build();
    }

    @Test
    @DisplayName("창고가 활성이고 아직 배정되지 않은 사용자면 관리자 배정에 성공한다")
    void assignManager_success() {
        WarehouseMemberAssignCommand command = new WarehouseMemberAssignCommand(1L, 10L, "MANAGER");
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(activeWarehouse));
        when(userUseCase.getUser(10L)).thenReturn(user(UserRole.WAREHOUSE_MANAGER, UserStatus.PENDING));
        when(warehouseMemberRepository.existsByWarehouseIdAndUserId(1L, 10L)).thenReturn(false);
        when(warehouseMemberRepository.save(any(WarehouseMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WarehouseMemberView result = warehouseMemberService.assignManager(command);

        assertThat(result.warehouseId()).isEqualTo(1L);
        assertThat(result.userId()).isEqualTo(10L);
        assertThat(result.memberRole()).isEqualTo("MANAGER");
        assertThat(result.userName()).isEqualTo("김담당");
        assertThat(result.loginId()).isEqualTo("member01");
    }

    @Test
    @DisplayName("허용되지 않은 담당 역할이면 VALIDATION_ERROR 예외를 던진다")
    void assignManager_invalidRole() {
        WarehouseMemberAssignCommand command = new WarehouseMemberAssignCommand(1L, 10L, "INVALID_ROLE");

        assertThatThrownBy(() -> warehouseMemberService.assignManager(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.VALIDATION_ERROR.name());
        verify(warehouseRepository, never()).findById(any());
    }

    @Test
    @DisplayName("존재하지 않는 창고에 관리자를 배정하면 WAREHOUSE_NOT_FOUND 예외를 던진다")
    void assignManager_warehouseNotFound() {
        WarehouseMemberAssignCommand command = new WarehouseMemberAssignCommand(999L, 10L, "MANAGER");
        when(warehouseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseMemberService.assignManager(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(WarehouseErrorCode.WAREHOUSE_NOT_FOUND.name());
    }

    @Test
    @DisplayName("비활성 창고에 관리자를 배정하면 CONFLICT 예외를 던진다")
    void assignManager_inactiveWarehouse() {
        Warehouse inactiveWarehouse =
                Warehouse.register("WH-002", "부산 물류센터", "부산시 해운대구", "051-1234-5678", BigDecimal.TEN);
        inactiveWarehouse.deactivate();
        WarehouseMemberAssignCommand command = new WarehouseMemberAssignCommand(2L, 10L, "MANAGER");
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(inactiveWarehouse));

        assertThatThrownBy(() -> warehouseMemberService.assignManager(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.CONFLICT.name());
        verify(warehouseMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 해당 창고에 배정된 사용자면 ALREADY_ASSIGNED 예외를 던진다")
    void assignManager_alreadyAssigned() {
        WarehouseMemberAssignCommand command = new WarehouseMemberAssignCommand(1L, 10L, "MANAGER");
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(activeWarehouse));
        when(userUseCase.getUser(10L)).thenReturn(user(UserRole.WAREHOUSE_MANAGER, UserStatus.ACTIVE));
        when(warehouseMemberRepository.existsByWarehouseIdAndUserId(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> warehouseMemberService.assignManager(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(WarehouseErrorCode.ALREADY_ASSIGNED.name());
        verify(warehouseMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 사용자면 USER_NOT_FOUND 예외를 던지고 배정하지 않는다")
    void assignManager_userNotFound() {
        WarehouseMemberAssignCommand command = new WarehouseMemberAssignCommand(1L, 10L, "MANAGER");
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(activeWarehouse));
        when(userUseCase.getUser(10L)).thenThrow(new BusinessException(AuthErrorCode.USER_NOT_FOUND));

        assertThatThrownBy(() -> warehouseMemberService.assignManager(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(AuthErrorCode.USER_NOT_FOUND.name());
        verify(warehouseMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("대상 사용자의 역할이 WAREHOUSE_MANAGER이 아니면 VALIDATION_ERROR 예외를 던진다")
    void assignManager_wrongUserRole() {
        WarehouseMemberAssignCommand command = new WarehouseMemberAssignCommand(1L, 10L, "MANAGER");
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(activeWarehouse));
        when(userUseCase.getUser(10L)).thenReturn(user(UserRole.HQ_ADMIN, UserStatus.ACTIVE));

        assertThatThrownBy(() -> warehouseMemberService.assignManager(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.VALIDATION_ERROR.name());
        verify(warehouseMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("INACTIVE 사용자면 CONFLICT 예외를 던진다")
    void assignManager_inactiveUser() {
        WarehouseMemberAssignCommand command = new WarehouseMemberAssignCommand(1L, 10L, "MANAGER");
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(activeWarehouse));
        when(userUseCase.getUser(10L)).thenReturn(user(UserRole.WAREHOUSE_MANAGER, UserStatus.INACTIVE));

        assertThatThrownBy(() -> warehouseMemberService.assignManager(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.CONFLICT.name());
        verify(warehouseMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("warehouseId·userId·keyword를 그대로 리포지토리에 전달한다")
    void getManagers_passesFilters() {
        WarehouseMemberView member = new WarehouseMemberView(5L, 1L, 10L, "김담당", "member01", "MANAGER", null);
        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(warehouseMemberRepository.search(1L, 10L, "김")).thenReturn(List.of(member));

        List<WarehouseMemberView> result = warehouseMemberService.getManagers(1L, 10L, "김");

        assertThat(result).hasSize(1);
        verify(warehouseMemberRepository).search(eq(1L), eq(10L), eq("김"));
    }

    @Test
    @DisplayName("존재하지 않는 창고로 필터링하면 WAREHOUSE_NOT_FOUND 예외를 던진다")
    void getManagers_warehouseNotFound() {
        when(warehouseRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> warehouseMemberService.getManagers(999L, null, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(WarehouseErrorCode.WAREHOUSE_NOT_FOUND.name());
        verify(warehouseMemberRepository, never()).search(any(), any(), any());
    }

    @Test
    @DisplayName("warehouseId·userId·keyword가 없으면 전체 배정을 조회한다")
    void getManagers_withoutFilters_returnsAll() {
        WarehouseMemberView member = new WarehouseMemberView(5L, 1L, 10L, "김담당", "member01", "MANAGER", null);
        when(warehouseMemberRepository.search(null, null, null)).thenReturn(List.of(member));

        List<WarehouseMemberView> result = warehouseMemberService.getManagers(null, null, null);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("존재하지 않는 배정을 해제하면 MEMBER_NOT_FOUND 예외를 던진다")
    void releaseManager_notFound() {
        when(warehouseMemberRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseMemberService.releaseManager(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(WarehouseErrorCode.MEMBER_NOT_FOUND.name());
        verify(warehouseMemberRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("존재하는 배정을 해제하면 삭제된다")
    void releaseManager_success() {
        WarehouseMember member = WarehouseMember.assign(1L, 10L, "MANAGER", null);
        when(warehouseMemberRepository.findById(1L)).thenReturn(Optional.of(member));

        warehouseMemberService.releaseManager(1L);

        verify(warehouseMemberRepository).deleteById(1L);
    }
}
