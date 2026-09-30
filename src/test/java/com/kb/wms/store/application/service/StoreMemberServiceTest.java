package com.kb.wms.store.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.store.application.port.in.command.StoreMemberAssignCommand;
import com.kb.wms.store.application.port.out.StoreMemberRepository;
import com.kb.wms.store.application.port.out.StoreRepository;
import com.kb.wms.store.domain.entity.Store;
import com.kb.wms.store.domain.entity.StoreMember;
import com.kb.wms.store.exception.StoreErrorCode;

@ExtendWith(MockitoExtension.class)
class StoreMemberServiceTest {

    @Mock
    private StoreMemberRepository storeMemberRepository;
    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private StoreMemberService storeMemberService;

    private final Store activeStore =
            Store.register("ST-GANGNAM", "강남점", "서울시 강남구", "김점주", "02-333-1234");

    @Test
    @DisplayName("지점이 활성이고 아직 배정되지 않은 사용자면 관리자 배정에 성공한다")
    void assignManager_success() {
        StoreMemberAssignCommand command = new StoreMemberAssignCommand(1L, 10L, "OWNER");
        when(storeRepository.findById(1L)).thenReturn(Optional.of(activeStore));
        when(storeMemberRepository.existsByStoreIdAndUserId(1L, 10L)).thenReturn(false);
        when(storeMemberRepository.save(any(StoreMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StoreMember result = storeMemberService.assignManager(command);

        assertThat(result.getStoreId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(10L);
        assertThat(result.getMemberRole()).isEqualTo("OWNER");
    }

    @Test
    @DisplayName("허용되지 않은 담당 역할이면 VALIDATION_ERROR 예외를 던진다")
    void assignManager_invalidRole() {
        StoreMemberAssignCommand command = new StoreMemberAssignCommand(1L, 10L, "INVALID_ROLE");

        assertThatThrownBy(() -> storeMemberService.assignManager(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.VALIDATION_ERROR.name());
        verify(storeRepository, never()).findById(any());
    }

    @Test
    @DisplayName("존재하지 않는 지점에 관리자를 배정하면 STORE_NOT_FOUND 예외를 던진다")
    void assignManager_storeNotFound() {
        StoreMemberAssignCommand command = new StoreMemberAssignCommand(999L, 10L, "OWNER");
        when(storeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> storeMemberService.assignManager(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(StoreErrorCode.STORE_NOT_FOUND.name());
    }

    @Test
    @DisplayName("비활성 지점에 관리자를 배정하면 CONFLICT 예외를 던진다")
    void assignManager_inactiveStore() {
        Store inactiveStore = Store.register("ST-HONGDAE", "홍대점", "서울시 마포구", "박점주", "02-444-5678");
        inactiveStore.deactivate();
        StoreMemberAssignCommand command = new StoreMemberAssignCommand(2L, 10L, "OWNER");
        when(storeRepository.findById(2L)).thenReturn(Optional.of(inactiveStore));

        assertThatThrownBy(() -> storeMemberService.assignManager(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.CONFLICT.name());
        verify(storeMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 해당 지점에 배정된 사용자면 ALREADY_ASSIGNED 예외를 던진다")
    void assignManager_alreadyAssigned() {
        StoreMemberAssignCommand command = new StoreMemberAssignCommand(1L, 10L, "OWNER");
        when(storeRepository.findById(1L)).thenReturn(Optional.of(activeStore));
        when(storeMemberRepository.existsByStoreIdAndUserId(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> storeMemberService.assignManager(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(StoreErrorCode.ALREADY_ASSIGNED.name());
        verify(storeMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("storeId·userId 필터를 그대로 리포지토리에 전달한다")
    void getManagers_passesFilters() {
        StoreMember member = StoreMember.assign(1L, 10L, "OWNER", null);
        when(storeRepository.existsById(1L)).thenReturn(true);
        when(storeMemberRepository.findAll(1L, 10L)).thenReturn(List.of(member));

        List<StoreMember> result = storeMemberService.getManagers(1L, 10L);

        assertThat(result).hasSize(1);
        verify(storeMemberRepository).findAll(eq(1L), eq(10L));
    }

    @Test
    @DisplayName("존재하지 않는 지점으로 필터링하면 STORE_NOT_FOUND 예외를 던진다")
    void getManagers_storeNotFound() {
        when(storeRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> storeMemberService.getManagers(999L, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(StoreErrorCode.STORE_NOT_FOUND.name());
        verify(storeMemberRepository, never()).findAll(any(), any());
    }

    @Test
    @DisplayName("storeId·userId가 없으면 전체 배정을 조회한다")
    void getManagers_withoutFilters_returnsAll() {
        StoreMember member = StoreMember.assign(1L, 10L, "OWNER", null);
        when(storeMemberRepository.findAll(null, null)).thenReturn(List.of(member));

        List<StoreMember> result = storeMemberService.getManagers(null, null);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("존재하지 않는 배정을 해제하면 MEMBER_NOT_FOUND 예외를 던진다")
    void releaseManager_notFound() {
        when(storeMemberRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> storeMemberService.releaseManager(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(StoreErrorCode.MEMBER_NOT_FOUND.name());
        verify(storeMemberRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("존재하는 배정을 해제하면 삭제된다")
    void releaseManager_success() {
        StoreMember member = StoreMember.assign(1L, 10L, "OWNER", null);
        when(storeMemberRepository.findById(1L)).thenReturn(Optional.of(member));

        storeMemberService.releaseManager(1L);

        verify(storeMemberRepository).deleteById(1L);
    }
}
