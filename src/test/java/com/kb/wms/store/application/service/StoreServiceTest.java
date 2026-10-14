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

import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.common.locationcode.LocationCodeIssuer;
import com.kb.wms.common.locationcode.LocationType;
import com.kb.wms.common.locationcode.RegionCode;
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.statushistory.application.port.in.StatusHistoryUseCase;
import com.kb.wms.store.application.port.in.command.StoreRegisterCommand;
import com.kb.wms.store.application.port.in.command.StoreUpdateCommand;
import com.kb.wms.store.application.port.in.query.StoreSearchCondition;
import com.kb.wms.store.application.port.in.result.StoreMembershipSummary;
import com.kb.wms.store.application.port.out.StoreMemberRepository;
import com.kb.wms.store.application.port.out.StoreOrderPresencePort;
import com.kb.wms.store.application.port.out.StoreRepository;
import com.kb.wms.store.domain.entity.Store;
import com.kb.wms.store.domain.entity.StoreMember;
import com.kb.wms.store.domain.enums.StoreHistoryEntityType;
import com.kb.wms.store.exception.StoreErrorCode;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @Mock
    private StoreRepository storeRepository;
    @Mock
    private StoreMemberRepository storeMemberRepository;
    @Mock
    private StoreOrderPresencePort storeOrderPresencePort;
    @Mock
    private StatusHistoryUseCase statusHistoryUseCase;
    @Mock
    private LocationCodeIssuer locationCodeIssuer;

    @InjectMocks
    private StoreService storeService;

    private final StoreRegisterCommand registerCommand =
            new StoreRegisterCommand("강남점", "서울 강남구 테헤란로 100", "김점주", "02-333-1234");

    @Test
    @DisplayName("등록하면 주소의 시·도로 발급한 코드가 지점 코드가 된다")
    void registerStore_issuesCodeFromAddressRegion() {
        when(locationCodeIssuer.issue(eq(LocationType.STORE), eq(RegionCode.SU), any())).thenReturn("ST-SU-001");
        when(storeRepository.save(any(Store.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Store result = storeService.registerStore(registerCommand);

        assertThat(result.getStoreCode()).isEqualTo("ST-SU-001");
        assertThat(result.isActive()).isTrue();
    }

    @Test
    @DisplayName("주소가 시·도로 시작하지 않으면 VALIDATION_ERROR이고 저장하지 않는다")
    void registerStore_addressWithoutRegion() {
        StoreRegisterCommand command = new StoreRegisterCommand("강남점", "강남구 테헤란로 100", "김점주", "02-333-1234");

        assertThatThrownBy(() -> storeService.registerStore(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.VALIDATION_ERROR.name());
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
    @DisplayName("같은 시·도 안에서 주소를 바꾸면 발급된 코드를 유지한다")
    void updateStore_sameRegion_keepsCode() {
        Store existing = Store.register("ST-SU-001", "강남점", "서울 강남구", "김점주", "02-333-1234");
        when(storeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(storeRepository.save(any(Store.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Store result = storeService.updateStore(1L,
                new StoreUpdateCommand(null, "서울특별시 송파구 올림픽로 1", null, null));

        assertThat(result.getStoreCode()).isEqualTo("ST-SU-001");
        verify(locationCodeIssuer, never()).issue(any(), any(), any());
    }

    @Test
    @DisplayName("다른 시·도로 주소를 바꾸면 새 시·도의 코드를 다시 발급한다")
    void updateStore_otherRegion_reissuesCode() {
        Store existing = Store.register("ST-SU-001", "강남점", "서울 강남구", "김점주", "02-333-1234");
        when(storeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(locationCodeIssuer.issue(eq(LocationType.STORE), eq(RegionCode.GG), any())).thenReturn("ST-GG-004");
        when(storeRepository.save(any(Store.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Store result = storeService.updateStore(1L,
                new StoreUpdateCommand(null, "경기도 성남시 분당구 판교로 1", null, null));

        assertThat(result.getStoreCode()).isEqualTo("ST-GG-004");
    }

    @Test
    @DisplayName("과거에 직접 입력한 코드는 다른 시·도로 주소를 바꿔도 유지한다")
    void updateStore_legacyCode_keptEvenWhenRegionChanges() {
        Store existing = Store.register("ST-GANGNAM", "강남점", "서울 강남구", "김점주", "02-333-1234");
        when(storeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(storeRepository.save(any(Store.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Store result = storeService.updateStore(1L,
                new StoreUpdateCommand(null, "경기도 성남시 분당구 판교로 1", null, null));

        assertThat(result.getStoreCode()).isEqualTo("ST-GANGNAM");
        verify(locationCodeIssuer, never()).issue(any(), any(), any());
    }

    @Test
    @DisplayName("존재하지 않는 지점을 비활성화하면 STORE_NOT_FOUND 예외를 던진다")
    void deactivateStore_notFound() {
        when(storeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> storeService.deactivateStore(999L, null, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(StoreErrorCode.STORE_NOT_FOUND.name());
    }

    @Test
    @DisplayName("진행 중인 발주가 있는 지점을 비활성화하면 STORE_IN_USE 예외를 던지고 저장하지 않는다")
    void deactivateStore_inProgressOrder_throwsStoreInUse() {
        Store active = Store.register("ST-GANGNAM", "강남점", "서울시 강남구", "김점주", "02-333-1234");
        when(storeRepository.findById(1L)).thenReturn(Optional.of(active));
        when(storeOrderPresencePort.hasInProgressOrders(1L)).thenReturn(true);

        assertThatThrownBy(() -> storeService.deactivateStore(1L, null, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(StoreErrorCode.STORE_IN_USE.name());
        assertThat(active.isActive()).isTrue();
        verify(storeRepository, never()).save(any());
        verify(statusHistoryUseCase, never()).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("이미 비활성화된 지점을 다시 비활성화하면 CONFLICT 예외를 던진다")
    void deactivateStore_alreadyInactive_throwsConflict() {
        Store inactive = Store.register("ST-GANGNAM", "강남점", "서울시 강남구", "김점주", "02-333-1234");
        inactive.deactivate();
        when(storeRepository.findById(1L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> storeService.deactivateStore(1L, null, 1L))
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

        Store result = storeService.deactivateStore(1L, "폐점", 5L);

        assertThat(result.isActive()).isFalse();
        verify(statusHistoryUseCase).record(StoreHistoryEntityType.STORE.name(), 1L, "ACTIVE", "INACTIVE", "폐점", 5L);
    }

    @Test
    @DisplayName("500자를 넘는 사유로 비활성화하면 VALIDATION_ERROR 예외를 던진다")
    void deactivateStore_reasonTooLong() {
        assertThatThrownBy(() -> storeService.deactivateStore(1L, "가".repeat(501), 5L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.VALIDATION_ERROR.name());
        verify(storeRepository, never()).save(any());
    }

    @Test
    @DisplayName("비활성 지점을 활성화하면 상태 이력에 기록한다")
    void activateStore_success() {
        Store inactive = Store.register("ST-GANGNAM", "강남점", "서울시 강남구", "김점주", "02-333-1234");
        inactive.deactivate();
        when(storeRepository.findById(1L)).thenReturn(Optional.of(inactive));
        when(storeRepository.save(any(Store.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Store result = storeService.activateStore(1L, 5L);

        assertThat(result.isActive()).isTrue();
        verify(statusHistoryUseCase).record(StoreHistoryEntityType.STORE.name(), 1L, "INACTIVE", "ACTIVE", null, 5L);
    }

    @Test
    @DisplayName("존재하지 않는 지점을 활성화하면 STORE_NOT_FOUND 예외를 던진다")
    void activateStore_notFound() {
        when(storeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> storeService.activateStore(999L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(StoreErrorCode.STORE_NOT_FOUND.name());
    }

    @Test
    @DisplayName("이미 활성인 지점을 활성화하면 CONFLICT 예외를 던지고 이력을 남기지 않는다")
    void activateStore_alreadyActive_throwsConflict() {
        Store active = Store.register("ST-GANGNAM", "강남점", "서울시 강남구", "김점주", "02-333-1234");
        when(storeRepository.findById(1L)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> storeService.activateStore(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.CONFLICT.name());
        verify(storeRepository, never()).save(any());
        verify(statusHistoryUseCase, never()).record(any(), any(), any(), any(), any(), any());
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

    private final AuthenticatedUser owner =
            new AuthenticatedUser(3L, UserRole.STORE_OWNER, List.of(), List.of(1L));
    private final AuthenticatedUser hqAdmin = new AuthenticatedUser(1L, UserRole.HQ_ADMIN, List.of(), List.of());

    @Test
    @DisplayName("점주는 담당 지점만 조회할 수 있고, 담당이 아니면 조회 전에 403 FORBIDDEN이다. 본사는 전체를 조회한다")
    void getStore_withActor_checksAssignedStore() {
        Store store = Store.register("ST-GANGNAM", "강남점", "서울시 강남구", "김점주", "02-333-1234");
        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
        when(storeRepository.findById(2L)).thenReturn(Optional.of(store));

        assertThat(storeService.getStore(1L, owner)).isSameAs(store);
        assertThat(storeService.getStore(2L, hqAdmin)).isSameAs(store);
        assertThatThrownBy(() -> storeService.getStore(2L, owner))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
        verify(storeRepository, never()).findById(3L);
    }
}
