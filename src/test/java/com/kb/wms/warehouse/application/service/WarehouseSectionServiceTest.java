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
import com.kb.wms.warehouse.application.port.in.command.WarehouseSectionRegisterCommand;
import com.kb.wms.warehouse.application.port.in.command.WarehouseSectionUpdateCommand;
import com.kb.wms.warehouse.application.port.in.query.WarehouseSectionSearchCondition;
import com.kb.wms.warehouse.application.port.out.StockPresencePort;
import com.kb.wms.warehouse.application.port.out.WarehouseRepository;
import com.kb.wms.warehouse.application.port.out.WarehouseSectionRepository;
import com.kb.wms.warehouse.domain.entity.Warehouse;
import com.kb.wms.warehouse.domain.entity.WarehouseSection;
import com.kb.wms.warehouse.exception.WarehouseErrorCode;

@ExtendWith(MockitoExtension.class)
class WarehouseSectionServiceTest {

    @Mock
    private WarehouseSectionRepository warehouseSectionRepository;
    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private StockPresencePort stockPresencePort;

    @InjectMocks
    private WarehouseSectionService warehouseSectionService;

    private final Warehouse activeWarehouse =
            Warehouse.register("WH-001", "서울 물류센터", "서울시 강남구", "02-1234-5678", BigDecimal.valueOf(1000));

    @Test
    @DisplayName("창고가 활성이고 구역 코드가 중복되지 않으면 등록에 성공한다")
    void registerSection_success() {
        WarehouseSectionRegisterCommand command =
                new WarehouseSectionRegisterCommand(1L, null, "A-01", "1구역", "ZONE", BigDecimal.valueOf(100));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(activeWarehouse));
        when(warehouseSectionRepository.existsByWarehouseIdAndSectionCode(1L, "A-01")).thenReturn(false);
        when(warehouseSectionRepository.save(any(WarehouseSection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WarehouseSection result = warehouseSectionService.registerSection(command);

        assertThat(result.getSectionCode()).isEqualTo("A-01");
        assertThat(result.isActive()).isTrue();
        assertThat(result.isRoot()).isTrue();
    }

    @Test
    @DisplayName("허용되지 않은 구역 유형이면 VALIDATION_ERROR 예외를 던진다")
    void registerSection_invalidSectionType() {
        WarehouseSectionRegisterCommand command =
                new WarehouseSectionRegisterCommand(1L, null, "A-01", "1구역", "INVALID_TYPE", BigDecimal.valueOf(100));

        assertThatThrownBy(() -> warehouseSectionService.registerSection(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.VALIDATION_ERROR.name());
        verify(warehouseRepository, never()).findById(any());
    }

    @Test
    @DisplayName("존재하지 않는 창고에 구역을 등록하면 WAREHOUSE_NOT_FOUND 예외를 던진다")
    void registerSection_warehouseNotFound() {
        WarehouseSectionRegisterCommand command =
                new WarehouseSectionRegisterCommand(999L, null, "A-01", "1구역", "ZONE", BigDecimal.valueOf(100));
        when(warehouseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseSectionService.registerSection(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(WarehouseErrorCode.WAREHOUSE_NOT_FOUND.name());
    }

    @Test
    @DisplayName("비활성 창고에 구역을 등록하면 CONFLICT 예외를 던진다")
    void registerSection_inactiveWarehouse() {
        Warehouse inactiveWarehouse =
                Warehouse.register("WH-002", "부산 물류센터", "부산시 해운대구", "051-1234-5678", BigDecimal.TEN);
        inactiveWarehouse.deactivate();
        WarehouseSectionRegisterCommand command =
                new WarehouseSectionRegisterCommand(2L, null, "A-01", "1구역", "ZONE", BigDecimal.valueOf(100));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(inactiveWarehouse));

        assertThatThrownBy(() -> warehouseSectionService.registerSection(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.CONFLICT.name());
        verify(warehouseSectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("상위 구역과 같은 창고 소속이고 상위 구역이 활성이면 하위 구역 등록에 성공한다")
    void registerSection_withActiveParent_success() {
        WarehouseSection parent = WarehouseSection.register(1L, null, "A", "A구역", "ZONE", BigDecimal.valueOf(500));
        WarehouseSectionRegisterCommand command =
                new WarehouseSectionRegisterCommand(1L, 10L, "A-01", "A-1랙", "RACK", BigDecimal.valueOf(50));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(activeWarehouse));
        when(warehouseSectionRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(warehouseSectionRepository.existsByWarehouseIdAndSectionCode(1L, "A-01")).thenReturn(false);
        when(warehouseSectionRepository.save(any(WarehouseSection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WarehouseSection result = warehouseSectionService.registerSection(command);

        assertThat(result.getParentSectionId()).isEqualTo(10L);
        assertThat(result.isRoot()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 상위 구역이면 PARENT_SECTION_NOT_FOUND 예외를 던진다")
    void registerSection_parentNotFound() {
        WarehouseSectionRegisterCommand command =
                new WarehouseSectionRegisterCommand(1L, 999L, "A-01", "1구역", "ZONE", BigDecimal.valueOf(100));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(activeWarehouse));
        when(warehouseSectionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseSectionService.registerSection(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(WarehouseErrorCode.PARENT_SECTION_NOT_FOUND.name());
    }

    @Test
    @DisplayName("상위 구역이 다른 창고 소속이면 VALIDATION_ERROR 예외를 던진다")
    void registerSection_parentInDifferentWarehouse() {
        WarehouseSection parentInOtherWarehouse =
                WarehouseSection.register(2L, null, "B", "B구역", "ZONE", BigDecimal.valueOf(500));
        WarehouseSectionRegisterCommand command =
                new WarehouseSectionRegisterCommand(1L, 10L, "A-01", "A-1랙", "RACK", BigDecimal.valueOf(50));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(activeWarehouse));
        when(warehouseSectionRepository.findById(10L)).thenReturn(Optional.of(parentInOtherWarehouse));

        assertThatThrownBy(() -> warehouseSectionService.registerSection(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.VALIDATION_ERROR.name());
    }

    @Test
    @DisplayName("상위 구역이 비활성이면 CONFLICT 예외를 던진다")
    void registerSection_inactiveParent() {
        WarehouseSection inactiveParent = WarehouseSection.register(1L, null, "A", "A구역", "ZONE", BigDecimal.valueOf(500));
        inactiveParent.deactivate();
        WarehouseSectionRegisterCommand command =
                new WarehouseSectionRegisterCommand(1L, 10L, "A-01", "A-1랙", "RACK", BigDecimal.valueOf(50));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(activeWarehouse));
        when(warehouseSectionRepository.findById(10L)).thenReturn(Optional.of(inactiveParent));

        assertThatThrownBy(() -> warehouseSectionService.registerSection(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.CONFLICT.name());
    }

    @Test
    @DisplayName("구역 코드가 같은 창고 내에서 중복되면 DUPLICATE_SECTION_CODE 예외를 던진다")
    void registerSection_duplicateSectionCode() {
        WarehouseSectionRegisterCommand command =
                new WarehouseSectionRegisterCommand(1L, null, "A-01", "1구역", "ZONE", BigDecimal.valueOf(100));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(activeWarehouse));
        when(warehouseSectionRepository.existsByWarehouseIdAndSectionCode(1L, "A-01")).thenReturn(true);

        assertThatThrownBy(() -> warehouseSectionService.registerSection(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(WarehouseErrorCode.DUPLICATE_SECTION_CODE.name());
        verify(warehouseSectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("조건이 모두 비어 있으면 전체 구역을 조회한다")
    void getSections_withoutWarehouseId_returnsAll() {
        WarehouseSection section = WarehouseSection.register(1L, null, "A", "A구역", "ZONE", BigDecimal.TEN);
        WarehouseSectionSearchCondition condition = new WarehouseSectionSearchCondition(null, null, null, null, null);
        when(warehouseSectionRepository.search(condition)).thenReturn(List.of(section));

        List<WarehouseSection> result = warehouseSectionService.getSections(condition);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("창고·상위 구역·유형·keyword·isActive 조건을 리포지토리에 전달한다")
    void getSections_withWarehouseId_filtersByWarehouse() {
        WarehouseSection section = WarehouseSection.register(1L, null, "A", "A구역", "ZONE", BigDecimal.TEN);
        WarehouseSectionSearchCondition condition =
                new WarehouseSectionSearchCondition(1L, 10L, "RACK", "A", true);
        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(warehouseSectionRepository.findById(10L)).thenReturn(Optional.of(section));
        when(warehouseSectionRepository.search(condition)).thenReturn(List.of(section));

        List<WarehouseSection> result = warehouseSectionService.getSections(condition);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getWarehouseId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("존재하지 않는 창고로 필터링하면 WAREHOUSE_NOT_FOUND 예외를 던진다")
    void getSections_warehouseNotFound() {
        when(warehouseRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> warehouseSectionService.getSections(
                new WarehouseSectionSearchCondition(999L, null, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(WarehouseErrorCode.WAREHOUSE_NOT_FOUND.name());
        verify(warehouseSectionRepository, never()).search(any());
    }

    @Test
    @DisplayName("존재하지 않는 상위 구역으로 필터링하면 PARENT_SECTION_NOT_FOUND 예외를 던진다")
    void getSections_parentNotFound() {
        when(warehouseSectionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseSectionService.getSections(
                new WarehouseSectionSearchCondition(null, 999L, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(WarehouseErrorCode.PARENT_SECTION_NOT_FOUND.name());
    }

    @Test
    @DisplayName("허용되지 않은 구역 유형으로 필터링하면 VALIDATION_ERROR 예외를 던진다")
    void getSections_invalidSectionType() {
        assertThatThrownBy(() -> warehouseSectionService.getSections(
                new WarehouseSectionSearchCondition(null, null, "INVALID_TYPE", null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.VALIDATION_ERROR.name());
    }

    @Test
    @DisplayName("존재하지 않는 구역을 조회하면 SECTION_NOT_FOUND 예외를 던진다")
    void getSection_notFound() {
        when(warehouseSectionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseSectionService.getSection(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(WarehouseErrorCode.SECTION_NOT_FOUND.name());
    }

    @Test
    @DisplayName("변경할 필드가 없으면 VALIDATION_ERROR 예외를 던진다")
    void updateSection_noChanges_throwsBusinessException() {
        WarehouseSectionUpdateCommand command = new WarehouseSectionUpdateCommand(null, null, null, null);

        assertThatThrownBy(() -> warehouseSectionService.updateSection(1L, command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.VALIDATION_ERROR.name());
        verify(warehouseSectionRepository, never()).findByIdForUpdate(any());
    }

    @Test
    @DisplayName("존재하지 않는 구역을 수정하면 SECTION_NOT_FOUND 예외를 던진다")
    void updateSection_notFound() {
        WarehouseSectionUpdateCommand command = new WarehouseSectionUpdateCommand("A-02", null, null, null);
        when(warehouseSectionRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseSectionService.updateSection(999L, command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(WarehouseErrorCode.SECTION_NOT_FOUND.name());
    }

    @Test
    @DisplayName("변경하려는 구역 코드가 같은 창고 내에서 중복되면 DUPLICATE_SECTION_CODE 예외를 던진다")
    void updateSection_duplicateSectionCode() {
        WarehouseSection existing = WarehouseSection.register(1L, null, "A-01", "1구역", "ZONE", BigDecimal.valueOf(100));
        WarehouseSectionUpdateCommand command = new WarehouseSectionUpdateCommand("A-02", null, null, null);
        when(warehouseSectionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existing));
        when(warehouseSectionRepository.existsByWarehouseIdAndSectionCode(1L, "A-02")).thenReturn(true);

        assertThatThrownBy(() -> warehouseSectionService.updateSection(1L, command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(WarehouseErrorCode.DUPLICATE_SECTION_CODE.name());
    }

    @Test
    @DisplayName("허용되지 않은 구역 유형으로 변경하면 VALIDATION_ERROR 예외를 던진다")
    void updateSection_invalidSectionType() {
        WarehouseSection existing = WarehouseSection.register(1L, null, "A-01", "1구역", "ZONE", BigDecimal.valueOf(100));
        WarehouseSectionUpdateCommand command = new WarehouseSectionUpdateCommand(null, null, "INVALID_TYPE", null);
        when(warehouseSectionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> warehouseSectionService.updateSection(1L, command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.VALIDATION_ERROR.name());
    }

    @Test
    @DisplayName("수용량을 현재 사용 용량보다 작게 변경하면 CAPACITY_BELOW_USAGE 예외를 던진다")
    void updateSection_capacityBelowUsage() {
        WarehouseSection existing =
                WarehouseSection.register(1L, null, "A-01", "1구역", "ZONE", BigDecimal.valueOf(100));
        ReflectionTestUtils.setField(existing, "currentCapacity", BigDecimal.valueOf(80));
        WarehouseSectionUpdateCommand command = new WarehouseSectionUpdateCommand(null, null, null, BigDecimal.valueOf(50));
        when(warehouseSectionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> warehouseSectionService.updateSection(1L, command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(WarehouseErrorCode.CAPACITY_BELOW_USAGE.name());
    }

    @Test
    @DisplayName("일부 필드만 변경하면 해당 필드만 수정되어 저장된다")
    void updateSection_partialUpdate_success() {
        WarehouseSection existing = WarehouseSection.register(1L, null, "A-01", "1구역", "ZONE", BigDecimal.valueOf(100));
        WarehouseSectionUpdateCommand command = new WarehouseSectionUpdateCommand(null, "새 이름", null, null);
        when(warehouseSectionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existing));
        when(warehouseSectionRepository.save(any(WarehouseSection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WarehouseSection result = warehouseSectionService.updateSection(1L, command);

        assertThat(result.getName()).isEqualTo("새 이름");
        assertThat(result.getSectionCode()).isEqualTo("A-01");
    }

    @Test
    @DisplayName("존재하지 않는 구역을 비활성화하면 SECTION_NOT_FOUND 예외를 던진다")
    void deactivateSection_notFound() {
        when(warehouseSectionRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseSectionService.deactivateSection(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(WarehouseErrorCode.SECTION_NOT_FOUND.name());
    }

    @Test
    @DisplayName("이미 비활성화된 구역을 다시 비활성화하면 CONFLICT 예외를 던진다")
    void deactivateSection_alreadyInactive_throwsConflict() {
        WarehouseSection inactive = WarehouseSection.register(1L, null, "A-01", "1구역", "ZONE", BigDecimal.valueOf(100));
        inactive.deactivate();
        when(warehouseSectionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> warehouseSectionService.deactivateSection(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.CONFLICT.name());
        verify(warehouseSectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("활성 구역을 비활성화하면 성공한다")
    void deactivateSection_success() {
        WarehouseSection active = WarehouseSection.register(1L, null, "A-01", "1구역", "ZONE", BigDecimal.valueOf(100));
        when(warehouseSectionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(active));
        when(warehouseSectionRepository.save(any(WarehouseSection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WarehouseSection result = warehouseSectionService.deactivateSection(1L);

        assertThat(result.isActive()).isFalse();
    }

    @Test
    @DisplayName("재고(보유·할당)가 남아 있는 구역을 비활성화하면 SECTION_HAS_INVENTORY 예외를 던진다")
    void deactivateSection_hasInventory() {
        WarehouseSection active = WarehouseSection.register(1L, null, "A-01", "1구역", "ZONE", BigDecimal.valueOf(100));
        when(warehouseSectionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(active));
        when(stockPresencePort.hasStockInSection(1L)).thenReturn(true);

        assertThatThrownBy(() -> warehouseSectionService.deactivateSection(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(WarehouseErrorCode.SECTION_HAS_INVENTORY.name());
        verify(warehouseSectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("활성 하위 구역이 있는 구역을 비활성화하면 SECTION_HAS_CHILDREN 예외를 던진다")
    void deactivateSection_hasActiveChildren() {
        WarehouseSection active = WarehouseSection.register(1L, null, "A", "A구역", "ZONE", BigDecimal.valueOf(100));
        when(warehouseSectionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(active));
        when(stockPresencePort.hasStockInSection(1L)).thenReturn(false);
        when(warehouseSectionRepository.existsActiveChild(1L)).thenReturn(true);

        assertThatThrownBy(() -> warehouseSectionService.deactivateSection(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(WarehouseErrorCode.SECTION_HAS_CHILDREN.name());
        verify(warehouseSectionRepository, never()).save(any());
    }
}
