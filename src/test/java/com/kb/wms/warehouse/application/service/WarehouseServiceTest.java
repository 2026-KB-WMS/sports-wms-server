package com.kb.wms.warehouse.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import com.kb.wms.warehouse.application.port.in.WarehouseSectionCapacityUseCase;
import com.kb.wms.warehouse.application.port.in.command.WarehouseRegisterCommand;
import com.kb.wms.warehouse.application.port.in.command.WarehouseUpdateCommand;
import com.kb.wms.warehouse.application.port.in.query.WarehouseSearchCondition;
import com.kb.wms.warehouse.application.port.in.result.WarehouseMembershipSummary;
import com.kb.wms.warehouse.application.port.out.StockPresencePort;
import com.kb.wms.warehouse.application.port.out.WarehouseMemberRepository;
import com.kb.wms.warehouse.application.port.out.WarehouseRepository;
import com.kb.wms.warehouse.application.port.out.WarehouseSectionRepository;
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

    @InjectMocks
    private WarehouseService warehouseService;

    private final WarehouseRegisterCommand registerCommand =
            new WarehouseRegisterCommand("WH-001", "서울 물류센터", "서울시 강남구", "02-1234-5678", BigDecimal.valueOf(1000));

    @Test
    @DisplayName("창고 코드가 중복되지 않으면 등록에 성공한다")
    void registerWarehouse_success() {
        when(warehouseRepository.existsByWarehouseCode("WH-001")).thenReturn(false);
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Warehouse result = warehouseService.registerWarehouse(registerCommand);

        assertThat(result.getWarehouseCode()).isEqualTo("WH-001");
        assertThat(result.isActive()).isTrue();
    }

    @Test
    @DisplayName("창고 코드가 중복되면 DUPLICATE_WAREHOUSE_CODE 예외를 던진다")
    void registerWarehouse_duplicateCode() {
        when(warehouseRepository.existsByWarehouseCode("WH-001")).thenReturn(true);

        assertThatThrownBy(() -> warehouseService.registerWarehouse(registerCommand))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(WarehouseErrorCode.DUPLICATE_WAREHOUSE_CODE.name());
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
        verify(warehouseRepository, never()).findById(any());
    }

    @Test
    @DisplayName("존재하지 않는 창고를 수정하면 WAREHOUSE_NOT_FOUND 예외를 던진다")
    void updateWarehouse_notFound() {
        WarehouseUpdateCommand command = new WarehouseUpdateCommand("새 이름", null, null, null);
        when(warehouseRepository.findById(999L)).thenReturn(Optional.empty());

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
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Warehouse result = warehouseService.updateWarehouse(1L, command);

        assertThat(result.getName()).isEqualTo("새 이름");
        assertThat(result.getAddress()).isEqualTo("서울시 강남구");
        verify(warehouseRepository).save(existing);
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
}
