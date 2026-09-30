package com.kb.wms.store.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import com.kb.wms.store.application.port.in.command.StoreRegisterCommand;
import com.kb.wms.store.application.port.in.command.StoreUpdateCommand;
import com.kb.wms.store.application.port.in.query.StoreSearchCondition;
import com.kb.wms.store.application.port.in.result.StoreMembershipSummary;
import com.kb.wms.store.application.port.out.StoreMemberRepository;
import com.kb.wms.store.application.port.out.StoreRepository;
import com.kb.wms.store.domain.entity.Store;
import com.kb.wms.store.domain.entity.StoreMember;
import com.kb.wms.store.exception.StoreErrorCode;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @Mock
    private StoreRepository storeRepository;
    @Mock
    private StoreMemberRepository storeMemberRepository;

    @InjectMocks
    private StoreService storeService;

    private final StoreRegisterCommand registerCommand =
            new StoreRegisterCommand("ST-GANGNAM", "강남점", "서울시 강남구 테헤란로 100", "김점주", "02-333-1234");

    @Test
    @DisplayName("지점 코드가 중복되지 않으면 등록에 성공한다")
    void registerStore_success() {
        when(storeRepository.existsByStoreCode("ST-GANGNAM")).thenReturn(false);
        when(storeRepository.save(any(Store.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Store result = storeService.registerStore(registerCommand);

        assertThat(result.getStoreCode()).isEqualTo("ST-GANGNAM");
        assertThat(result.isActive()).isTrue();
    }

    @Test
    @DisplayName("지점 코드가 중복되면 DUPLICATE_STORE_CODE 예외를 던진다")
    void registerStore_duplicateCode() {
        when(storeRepository.existsByStoreCode("ST-GANGNAM")).thenReturn(true);

        assertThatThrownBy(() -> storeService.registerStore(registerCommand))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(StoreErrorCode.DUPLICATE_STORE_CODE.name());
        verify(storeRepository, never()).save(any());
    }

    @Test
    @DisplayName("지점 목록 조회는 검색 조건을 리포지토리에 전달해 결과를 그대로 반환한다")
    void getStores_returnsAll() {
        Store store = Store.register("ST-GANGNAM", "강남점", "서울시 강남구", "김점주", "02-333-1234");
        StoreSearchCondition condition = new StoreSearchCondition("강남", true);
        when(storeRepository.search(condition)).thenReturn(List.of(store));

        List<Store> result = storeService.getStores(condition);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStoreCode()).isEqualTo("ST-GANGNAM");
    }

    @Test
    @DisplayName("존재하지 않는 지점을 조회하면 STORE_NOT_FOUND 예외를 던진다")
    void getStore_notFound() {
        when(storeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> storeService.getStore(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(StoreErrorCode.STORE_NOT_FOUND.name());
    }

    @Test
    @DisplayName("변경할 필드가 없으면 VALIDATION_ERROR 예외를 던진다")
    void updateStore_noChanges_throwsBusinessException() {
        StoreUpdateCommand command = new StoreUpdateCommand(null, null, null, null);

        assertThatThrownBy(() -> storeService.updateStore(1L, command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.VALIDATION_ERROR.name());
        verify(storeRepository, never()).findById(any());
    }

    @Test
    @DisplayName("존재하지 않는 지점을 수정하면 STORE_NOT_FOUND 예외를 던진다")
    void updateStore_notFound() {
        StoreUpdateCommand command = new StoreUpdateCommand("새 이름", null, null, null);
        when(storeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> storeService.updateStore(999L, command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(StoreErrorCode.STORE_NOT_FOUND.name());
    }

    @Test
    @DisplayName("일부 필드만 변경하면 해당 필드만 수정되어 저장된다")
    void updateStore_partialUpdate_success() {
        Store existing = Store.register("ST-GANGNAM", "강남점", "서울시 강남구", "김점주", "02-333-1234");
        StoreUpdateCommand command = new StoreUpdateCommand("새 이름", null, null, null);
        when(storeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(storeRepository.save(any(Store.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Store result = storeService.updateStore(1L, command);

        assertThat(result.getName()).isEqualTo("새 이름");
        assertThat(result.getAddress()).isEqualTo("서울시 강남구");
        verify(storeRepository).save(existing);
    }

    @Test
    @DisplayName("존재하지 않는 지점을 비활성화하면 STORE_NOT_FOUND 예외를 던진다")
    void deactivateStore_notFound() {
        when(storeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> storeService.deactivateStore(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(StoreErrorCode.STORE_NOT_FOUND.name());
    }

    @Test
    @DisplayName("이미 비활성화된 지점을 다시 비활성화하면 CONFLICT 예외를 던진다")
    void deactivateStore_alreadyInactive_throwsConflict() {
        Store inactive = Store.register("ST-GANGNAM", "강남점", "서울시 강남구", "김점주", "02-333-1234");
        inactive.deactivate();
        when(storeRepository.findById(1L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> storeService.deactivateStore(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.CONFLICT.name());
        verify(storeRepository, never()).save(any());
    }

    @Test
    @DisplayName("활성 지점을 비활성화하면 성공한다")
    void deactivateStore_success() {
        Store active = Store.register("ST-GANGNAM", "강남점", "서울시 강남구", "김점주", "02-333-1234");
        when(storeRepository.findById(1L)).thenReturn(Optional.of(active));
        when(storeRepository.save(any(Store.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Store result = storeService.deactivateStore(1L);

        assertThat(result.isActive()).isFalse();
    }

    @Test
    @DisplayName("본인이 배정된 지점만 지점·배정 정보를 합쳐 반환한다")
    void getMyStores_returnsOnlyOwnMemberships() {
        Store store = Store.register("ST-GANGNAM", "강남점", "서울시 강남구", "김점주", "02-333-1234");
        StoreMember member = StoreMember.assign(1L, 10L, "OWNER", null);
        when(storeMemberRepository.findByUserId(10L)).thenReturn(List.of(member));
        when(storeRepository.findById(member.getStoreId())).thenReturn(Optional.of(store));

        List<StoreMembershipSummary> result = storeService.getMyStores(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).storeCode()).isEqualTo("ST-GANGNAM");
        assertThat(result.get(0).memberRole()).isEqualTo("OWNER");
    }

    @Test
    @DisplayName("배정된 지점이 없는 사용자는 오류 없이 빈 목록을 반환한다")
    void getMyStores_noMembership_returnsEmptyList() {
        when(storeMemberRepository.findByUserId(999L)).thenReturn(List.of());

        List<StoreMembershipSummary> result = storeService.getMyStores(999L);

        assertThat(result).isEmpty();
    }
}
