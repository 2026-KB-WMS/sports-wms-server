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
import org.springframework.test.util.ReflectionTestUtils;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.common.locationcode.LocationCodeIssuer;
import com.kb.wms.common.locationcode.LocationType;
import com.kb.wms.common.locationcode.RegionCode;
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.common.security.UserRole;
import com.kb.wms.warehouse.application.port.in.WarehouseSectionCapacityUseCase;
import com.kb.wms.warehouse.application.port.in.command.WarehouseRegisterCommand;
import com.kb.wms.warehouse.application.port.in.command.WarehouseUpdateCommand;
import com.kb.wms.warehouse.application.port.in.query.WarehouseSearchCondition;
import com.kb.wms.warehouse.application.port.in.result.WarehouseMembershipSummary;
import com.kb.wms.warehouse.application.port.out.StockPresencePort;
import com.kb.wms.warehouse.application.port.out.WarehouseMemberRepository;
import com.kb.wms.warehouse.application.port.out.WarehouseRepository;
import com.kb.wms.warehouse.application.port.out.WarehouseSectionRepository;
import com.kb.wms.warehouse.application.port.out.WarehouseUsagePort;
import com.kb.wms.warehouse.domain.entity.Warehouse;
import com.kb.wms.warehouse.domain.entity.WarehouseMember;
import com.kb.wms.warehouse.domain.entity.WarehouseSection;
import com.kb.wms.warehouse.exception.WarehouseErrorCode;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private WarehouseMemberRepository warehouseMemberRepository;
    @Mock
    private WarehouseSectionRepository warehouseSectionRepository;
    @Mock
    private WarehouseSectionCapacityUseCase warehouseSectionCapacityUseCase;
    @Mock
    private StockPresencePort stockPresencePort;
    @Mock
    private WarehouseUsagePort warehouseUsagePort;
    @Mock
    private LocationCodeIssuer locationCodeIssuer;

    @InjectMocks
    private WarehouseService warehouseService;

    private final WarehouseRegisterCommand registerCommand =
            new WarehouseRegisterCommand("서울 물류센터", "서울 강남구 테헤란로 1", "02-1234-5678", BigDecimal.valueOf(1000));

    @Test
    @DisplayName("등록하면 주소의 시·도로 발급한 코드가 창고 코드가 된다")
    void registerWarehouse_issuesCodeFromAddressRegion() {
        when(locationCodeIssuer.issue(eq(LocationType.WAREHOUSE), eq(RegionCode.SU), any()))
                .thenReturn("WH-SU-001");
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Warehouse result = warehouseService.registerWarehouse(registerCommand);

        assertThat(result.getWarehouseCode()).isEqualTo("WH-SU-001");
        assertThat(result.isActive()).isTrue();
    }

    @Test
    @DisplayName("주소가 시·도로 시작하지 않으면 VALIDATION_ERROR이고 저장하지 않는다")
    void registerWarehouse_addressWithoutRegion() {
        WarehouseRegisterCommand command =
                new WarehouseRegisterCommand("물류센터", "강남구 테헤란로 1", "02-1234-5678", BigDecimal.TEN);

        assertThatThrownBy(() -> warehouseService.registerWarehouse(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.VALIDATION_ERROR.name());
        verify(warehouseRepository, never()).save(any());
    }

    @Test
    @DisplayName("창고 목록 조회는 검색 조건을 리포지토리에 전달해 결과를 그대로 반환한다")
    void getWarehouses_returnsAll() {
        Warehouse warehouse = Warehouse.register("WH-001", "서울 물류센터", "서울시 강남구", "02-1234-5678", BigDecimal.TEN);
        WarehouseSearchCondition condition = new WarehouseSearchCondition("서울", true);
        when(warehouseRepository.search(condition)).thenReturn(List.of(warehouse));

        List<Warehouse> result = warehouseService.getWarehouses(condition);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getWarehouseCode()).isEqualTo("WH-001");
    }

    @Test
    @DisplayName("존재하지 않는 창고를 조회하면 WAREHOUSE_NOT_FOUND 예외를 던진다")
    void getWarehouse_notFound() {
        when(warehouseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseService.getWarehouse(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(WarehouseErrorCode.WAREHOUSE_NOT_FOUND.name());
    }

    @Test
    @DisplayName("변경할 필드가 없으면 VALIDATION_ERROR 예외를 던진다")
    void updateWarehouse_noChanges_throwsBusinessException() {
        WarehouseUpdateCommand command = new WarehouseUpdateCommand(null, null, null, null);

        assertThatThrownBy(() -> warehouseService.updateWarehouse(1L, command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.VALIDATION_ERROR.name());
        verify(warehouseRepository, never()).findByIdForUpdate(any());
    }

    @Test
    @DisplayName("존재하지 않는 창고를 수정하면 WAREHOUSE_NOT_FOUND 예외를 던진다")
    void updateWarehouse_notFound() {
        WarehouseUpdateCommand command = new WarehouseUpdateCommand("새 이름", null, null, null);
        when(warehouseRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseService.updateWarehouse(999L, command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(WarehouseErrorCode.WAREHOUSE_NOT_FOUND.name());
    }

    @Test
    @DisplayName("일부 필드만 변경하면 해당 필드만 수정되어 저장된다")
    void updateWarehouse_partialUpdate_success() {
        Warehouse existing = Warehouse.register("WH-001", "서울 물류센터", "서울시 강남구", "02-1234-5678", BigDecimal.TEN);
        WarehouseUpdateCommand command = new WarehouseUpdateCommand("새 이름", null, null, null);
        when(warehouseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existing));
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Warehouse result = warehouseService.updateWarehouse(1L, command);

        assertThat(result.getName()).isEqualTo("새 이름");
        assertThat(result.getAddress()).isEqualTo("서울시 강남구");
        verify(warehouseRepository).save(existing);
    }

    @Test
    @DisplayName("같은 시·도 안에서 주소를 바꾸면 발급된 코드를 유지한다")
    void updateWarehouse_sameRegion_keepsCode() {
        Warehouse existing = Warehouse.register("WH-SU-001", "서울 물류센터", "서울 강남구", "02-1234-5678", BigDecimal.TEN);
        when(warehouseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existing));
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Warehouse result = warehouseService.updateWarehouse(1L,
                new WarehouseUpdateCommand(null, "서울특별시 송파구 올림픽로 1", null, null));

        assertThat(result.getWarehouseCode()).isEqualTo("WH-SU-001");
        assertThat(result.getAddress()).isEqualTo("서울특별시 송파구 올림픽로 1");
        verify(locationCodeIssuer, never()).issue(any(), any(), any());
    }

    @Test
    @DisplayName("다른 시·도로 주소를 바꾸면 새 시·도의 코드를 다시 발급한다")
    void updateWarehouse_otherRegion_reissuesCode() {
        Warehouse existing = Warehouse.register("WH-SU-001", "서울 물류센터", "서울 강남구", "02-1234-5678", BigDecimal.TEN);
        when(warehouseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existing));
        when(locationCodeIssuer.issue(eq(LocationType.WAREHOUSE), eq(RegionCode.BS), any()))
                .thenReturn("WH-BS-003");
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Warehouse result = warehouseService.updateWarehouse(1L,
                new WarehouseUpdateCommand(null, "부산 해운대구 센텀로 1", null, null));

        assertThat(result.getWarehouseCode()).isEqualTo("WH-BS-003");
    }

    @Test
    @DisplayName("과거에 직접 입력한 코드는 다른 시·도로 주소를 바꿔도 유지한다")
    void updateWarehouse_legacyCode_keptEvenWhenRegionChanges() {
        Warehouse existing = Warehouse.register("WH-001", "서울 물류센터", "서울 강남구", "02-1234-5678", BigDecimal.TEN);
        when(warehouseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existing));
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Warehouse result = warehouseService.updateWarehouse(1L,
                new WarehouseUpdateCommand(null, "부산 해운대구 센텀로 1", null, null));

        assertThat(result.getWarehouseCode()).isEqualTo("WH-001");
        verify(locationCodeIssuer, never()).issue(any(), any(), any());
    }

    @Test
    @DisplayName("수정할 주소가 시·도로 시작하지 않으면 VALIDATION_ERROR이고 코드를 바꾸지 않는다")
    void updateWarehouse_addressWithoutRegion() {
        Warehouse existing = Warehouse.register("WH-SU-001", "서울 물류센터", "서울 강남구", "02-1234-5678", BigDecimal.TEN);
        when(warehouseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> warehouseService.updateWarehouse(1L,
                new WarehouseUpdateCommand(null, "테헤란로 1", null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.VALIDATION_ERROR.name());
        assertThat(existing.getWarehouseCode()).isEqualTo("WH-SU-001");
        verify(warehouseRepository, never()).save(any());
    }

    @Test
    @DisplayName("전체 수용량을 최상위 활성 구역 합보다 작게 변경하면 CAPACITY_BELOW_CHILDREN 예외를 던진다")
    void updateWarehouse_totalCapacityBelowSections() {
        Warehouse existing = Warehouse.register("WH-001", "서울 물류센터", "서울시 강남구", "02-1234-5678", BigDecimal.valueOf(200));
        WarehouseUpdateCommand command = new WarehouseUpdateCommand(null, null, null, BigDecimal.valueOf(99));
        when(warehouseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existing));
        when(warehouseSectionRepository.sumActiveCapacity(1L, null, null)).thenReturn(BigDecimal.valueOf(100));

        assertThatThrownBy(() -> warehouseService.updateWarehouse(1L, command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(WarehouseErrorCode.CAPACITY_BELOW_CHILDREN.name());
        verify(warehouseRepository, never()).save(any());
    }

    @Test
    @DisplayName("전체 수용량을 최상위 활성 구역 합과 같게 변경하는 것은 허용한다")
    void updateWarehouse_totalCapacityEqualToSections_success() {
        Warehouse existing = Warehouse.register("WH-001", "서울 물류센터", "서울시 강남구", "02-1234-5678", BigDecimal.valueOf(200));
        WarehouseUpdateCommand command = new WarehouseUpdateCommand(null, null, null, BigDecimal.valueOf(100));
        when(warehouseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existing));
        when(warehouseSectionRepository.sumActiveCapacity(1L, null, null)).thenReturn(BigDecimal.valueOf(100));
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(warehouseService.updateWarehouse(1L, command).getTotalCapacity()).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("존재하지 않는 창고를 비활성화하면 WAREHOUSE_NOT_FOUND 예외를 던진다")
    void deactivateWarehouse_notFound() {
        when(warehouseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseService.deactivateWarehouse(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(WarehouseErrorCode.WAREHOUSE_NOT_FOUND.name());
    }

    @Test
    @DisplayName("이미 비활성화된 창고를 다시 비활성화하면 CONFLICT 예외를 던진다")
    void deactivateWarehouse_alreadyInactive_throwsConflict() {
        Warehouse inactive = Warehouse.register("WH-001", "서울 물류센터", "서울시 강남구", "02-1234-5678", BigDecimal.TEN);
        inactive.deactivate();
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> warehouseService.deactivateWarehouse(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.CONFLICT.name());
        verify(warehouseRepository, never()).save(any());
    }

    @Test
    @DisplayName("활성 창고를 비활성화하면 성공한다")
    void deactivateWarehouse_success() {
        Warehouse active = Warehouse.register("WH-001", "서울 물류센터", "서울시 강남구", "02-1234-5678", BigDecimal.TEN);
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(active));
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Warehouse result = warehouseService.deactivateWarehouse(1L);

        assertThat(result.isActive()).isFalse();
    }

    @Test
    @DisplayName("비활성 창고를 활성화하면 성공한다")
    void activateWarehouse_success() {
        Warehouse inactive = Warehouse.register("WH-001", "서울 물류센터", "서울시 강남구", "02-1234-5678", BigDecimal.TEN);
        inactive.deactivate();
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(inactive));
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(warehouseService.activateWarehouse(1L).isActive()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 창고를 활성화하면 WAREHOUSE_NOT_FOUND 예외를 던진다")
    void activateWarehouse_notFound() {
        when(warehouseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseService.activateWarehouse(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(WarehouseErrorCode.WAREHOUSE_NOT_FOUND.name());
    }

    @Test
    @DisplayName("이미 활성인 창고를 활성화하면 CONFLICT 예외를 던진다")
    void activateWarehouse_alreadyActive_throwsConflict() {
        Warehouse active = Warehouse.register("WH-001", "서울 물류센터", "서울시 강남구", "02-1234-5678", BigDecimal.TEN);
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> warehouseService.activateWarehouse(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.CONFLICT.name());
        verify(warehouseRepository, never()).save(any());
    }

    @Test
    @DisplayName("창고를 비활성화할 때 창고의 구역 행을 잠근 뒤 재고가 남아 있으면 WAREHOUSE_IN_USE 예외를 던진다")
    void deactivateWarehouse_hasInventory() {
        Warehouse active = Warehouse.register("WH-001", "서울 물류센터", "서울시 강남구", "02-1234-5678", BigDecimal.TEN);
        WarehouseSection section = WarehouseSection.register(1L, null, "A", "A구역", "ZONE", BigDecimal.TEN);
        ReflectionTestUtils.setField(section, "sectionId", 7L);
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(active));
        when(warehouseSectionRepository.findAllByWarehouseId(1L)).thenReturn(List.of(section));
        when(stockPresencePort.hasStockInWarehouse(1L)).thenReturn(true);

        assertThatThrownBy(() -> warehouseService.deactivateWarehouse(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(WarehouseErrorCode.WAREHOUSE_IN_USE.name());
        verify(warehouseSectionCapacityUseCase).lock(List.of(7L));
        verify(warehouseRepository, never()).save(any());
    }

    @Test
    @DisplayName("진행 중 입고가 있으면 WAREHOUSE_IN_USE 예외를 던진다")
    void deactivateWarehouse_inProgressInbound() {
        assertBlockedBy(() -> when(warehouseUsagePort.hasInProgressInbounds(1L)).thenReturn(true));
    }

    @Test
    @DisplayName("진행 중 창고 발주가 있으면 WAREHOUSE_IN_USE 예외를 던진다")
    void deactivateWarehouse_inProgressPurchaseOrder() {
        assertBlockedBy(() -> when(warehouseUsagePort.hasInProgressPurchaseOrders(1L)).thenReturn(true));
    }

    @Test
    @DisplayName("진행 중 출고가 있으면 WAREHOUSE_IN_USE 예외를 던진다")
    void deactivateWarehouse_inProgressOutbound() {
        assertBlockedBy(() -> when(warehouseUsagePort.hasInProgressOutbounds(1L)).thenReturn(true));
    }

    @Test
    @DisplayName("배정된 진행 중 지점 발주가 있으면 WAREHOUSE_IN_USE 예외를 던진다")
    void deactivateWarehouse_inProgressStoreOrder() {
        assertBlockedBy(() -> when(warehouseUsagePort.hasInProgressStoreOrders(1L)).thenReturn(true));
    }

    private void assertBlockedBy(Runnable stubbing) {
        Warehouse active = Warehouse.register("WH-001", "서울 물류센터", "서울시 강남구", "02-1234-5678", BigDecimal.TEN);
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(active));
        when(warehouseSectionRepository.findAllByWarehouseId(1L)).thenReturn(List.of());
        stubbing.run();

        assertThatThrownBy(() -> warehouseService.deactivateWarehouse(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(WarehouseErrorCode.WAREHOUSE_IN_USE.name());
        assertThat(active.isActive()).isTrue();
        verify(warehouseRepository, never()).save(any());
    }

    @Test
    @DisplayName("본인이 소속된 창고만 창고·배정 정보를 합쳐 반환한다")
    void getMyWarehouses_returnsOnlyOwnMemberships() {
        Warehouse warehouse = Warehouse.register("WH-001", "서울 물류센터", "서울시 강남구", "02-1234-5678", BigDecimal.TEN);
        WarehouseMember member = WarehouseMember.assign(1L, 10L, "MANAGER", null);
        when(warehouseMemberRepository.findByUserId(10L)).thenReturn(List.of(member));
        when(warehouseRepository.findById(member.getWarehouseId())).thenReturn(Optional.of(warehouse));

        List<WarehouseMembershipSummary> result = warehouseService.getMyWarehouses(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).warehouseCode()).isEqualTo("WH-001");
        assertThat(result.get(0).memberRole()).isEqualTo("MANAGER");
    }


    private final AuthenticatedUser manager =
            new AuthenticatedUser(2L, UserRole.WAREHOUSE_MANAGER, List.of(1L), List.of());
    private final AuthenticatedUser hqAdmin = new AuthenticatedUser(1L, UserRole.HQ_ADMIN, List.of(), List.of());

    @Test
    @DisplayName("창고 관리자는 담당 창고만 조회할 수 있고, 담당이 아니면 조회 전에 403 FORBIDDEN이다. 본사는 전체를 조회한다")
    void getWarehouse_withActor_checksAssignedWarehouse() {
        Warehouse warehouse = Warehouse.register("WH-001", "서울 물류센터", "서울시 강남구", "02-1234-5678", BigDecimal.TEN);
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(warehouse));

        assertThat(warehouseService.getWarehouse(1L, manager)).isSameAs(warehouse);
        assertThat(warehouseService.getWarehouse(2L, hqAdmin)).isSameAs(warehouse);
        assertThatThrownBy(() -> warehouseService.getWarehouse(2L, manager))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
        verify(warehouseRepository, never()).findById(3L);
    }
}
