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

import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.auth.domain.enums.UserRole;
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
        when(warehouseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(activeWarehouse));
        when(warehouseSectionRepository.existsByWarehouseIdAndSectionCode(1L, "A-01")).thenReturn(false);
        when(warehouseSectionRepository.sumActiveCapacity(1L, null, null)).thenReturn(BigDecimal.ZERO);
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
        verify(warehouseRepository, never()).findByIdForUpdate(any());
    }

    @Test
    @DisplayName("존재하지 않는 창고에 구역을 등록하면 WAREHOUSE_NOT_FOUND 예외를 던진다")
    void registerSection_warehouseNotFound() {
        WarehouseSectionRegisterCommand command =
                new WarehouseSectionRegisterCommand(999L, null, "A-01", "1구역", "ZONE", BigDecimal.valueOf(100));
        when(warehouseRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

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
        when(warehouseRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(inactiveWarehouse));

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
        when(warehouseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(activeWarehouse));
        when(warehouseSectionRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(warehouseSectionRepository.existsByWarehouseIdAndSectionCode(1L, "A-01")).thenReturn(false);
        when(warehouseSectionRepository.sumActiveCapacity(1L, 10L, null)).thenReturn(BigDecimal.ZERO);
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
        when(warehouseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(activeWarehouse));
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
        when(warehouseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(activeWarehouse));
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
        when(warehouseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(activeWarehouse));
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
        when(warehouseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(activeWarehouse));
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

    @Test
    @DisplayName("창고가 활성이고 상위 구역이 없으면 비활성 구역을 활성화한다")
    void activateSection_root_success() {
        WarehouseSection inactive = WarehouseSection.register(1L, null, "A-01", "1구역", "ZONE", BigDecimal.valueOf(100));
        inactive.deactivate();
        when(warehouseSectionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(inactive));
        when(warehouseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(activeWarehouse));
        when(warehouseSectionRepository.sumActiveCapacity(1L, null, null)).thenReturn(BigDecimal.ZERO);
        when(warehouseSectionRepository.save(any(WarehouseSection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WarehouseSection result = warehouseSectionService.activateSection(1L);

        assertThat(result.isActive()).isTrue();
    }

    @Test
    @DisplayName("상위 구역이 활성이면 하위 구역을 활성화한다")
    void activateSection_withActiveParent_success() {
        WarehouseSection parent = WarehouseSection.register(1L, null, "A", "A구역", "ZONE", BigDecimal.valueOf(500));
        WarehouseSection child = WarehouseSection.register(1L, 10L, "A-01", "A-1랙", "RACK", BigDecimal.valueOf(50));
        child.deactivate();
        when(warehouseSectionRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(child));
        when(warehouseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(activeWarehouse));
        when(warehouseSectionRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(warehouseSectionRepository.sumActiveCapacity(1L, 10L, null)).thenReturn(BigDecimal.ZERO);
        when(warehouseSectionRepository.save(any(WarehouseSection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(warehouseSectionService.activateSection(11L).isActive()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 구역을 활성화하면 SECTION_NOT_FOUND 예외를 던진다")
    void activateSection_notFound() {
        when(warehouseSectionRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseSectionService.activateSection(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(WarehouseErrorCode.SECTION_NOT_FOUND.name());
    }

    @Test
    @DisplayName("이미 활성인 구역을 활성화하면 CONFLICT 예외를 던진다")
    void activateSection_alreadyActive_throwsConflict() {
        WarehouseSection active = WarehouseSection.register(1L, null, "A-01", "1구역", "ZONE", BigDecimal.valueOf(100));
        when(warehouseSectionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> warehouseSectionService.activateSection(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.CONFLICT.name());
        verify(warehouseSectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("창고가 비활성이면 구역을 활성화할 수 없다")
    void activateSection_inactiveWarehouse_throwsConflict() {
        WarehouseSection inactive = WarehouseSection.register(2L, null, "A-01", "1구역", "ZONE", BigDecimal.valueOf(100));
        inactive.deactivate();
        Warehouse inactiveWarehouse =
                Warehouse.register("WH-002", "부산 물류센터", "부산시 해운대구", "051-1234-5678", BigDecimal.TEN);
        inactiveWarehouse.deactivate();
        when(warehouseSectionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(inactive));
        when(warehouseRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(inactiveWarehouse));

        assertThatThrownBy(() -> warehouseSectionService.activateSection(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.CONFLICT.name());
        verify(warehouseSectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("상위 구역이 비활성이면 하위 구역을 활성화할 수 없다")
    void activateSection_inactiveParent_throwsConflict() {
        WarehouseSection inactiveParent =
                WarehouseSection.register(1L, null, "A", "A구역", "ZONE", BigDecimal.valueOf(500));
        inactiveParent.deactivate();
        WarehouseSection child = WarehouseSection.register(1L, 10L, "A-01", "A-1랙", "RACK", BigDecimal.valueOf(50));
        child.deactivate();
        when(warehouseSectionRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(child));
        when(warehouseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(activeWarehouse));
        when(warehouseSectionRepository.findById(10L)).thenReturn(Optional.of(inactiveParent));

        assertThatThrownBy(() -> warehouseSectionService.activateSection(11L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.CONFLICT.name());
        verify(warehouseSectionRepository, never()).save(any());
    }


    @Test
    @DisplayName("최상위 구역 수용량 합이 창고 전체 수용량을 넘으면 PARENT_CAPACITY_EXCEEDED 예외를 던진다")
    void registerSection_exceedsWarehouseTotalCapacity() {
        WarehouseSectionRegisterCommand command =
                new WarehouseSectionRegisterCommand(1L, null, "A-01", "1구역", "ZONE", BigDecimal.valueOf(100));
        when(warehouseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(activeWarehouse));
        when(warehouseSectionRepository.existsByWarehouseIdAndSectionCode(1L, "A-01")).thenReturn(false);
        when(warehouseSectionRepository.sumActiveCapacity(1L, null, null)).thenReturn(BigDecimal.valueOf(950));

        assertThatThrownBy(() -> warehouseSectionService.registerSection(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(WarehouseErrorCode.PARENT_CAPACITY_EXCEEDED.name());
        verify(warehouseSectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("형제 구역 합과 정확히 상위 구역 수용량이 같아지는 등록은 허용한다")
    void registerSection_equalToParentCapacity_success() {
        WarehouseSection parent = WarehouseSection.register(1L, null, "A", "A구역", "ZONE", BigDecimal.valueOf(500));
        WarehouseSectionRegisterCommand command =
                new WarehouseSectionRegisterCommand(1L, 10L, "A-02", "A-2랙", "RACK", BigDecimal.valueOf(100));
        when(warehouseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(activeWarehouse));
        when(warehouseSectionRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(warehouseSectionRepository.existsByWarehouseIdAndSectionCode(1L, "A-02")).thenReturn(false);
        when(warehouseSectionRepository.sumActiveCapacity(1L, 10L, null)).thenReturn(BigDecimal.valueOf(400));
        when(warehouseSectionRepository.save(any(WarehouseSection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(warehouseSectionService.registerSection(command).getCapacity()).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("하위 구역 수용량 합이 상위 구역 수용량을 넘으면 PARENT_CAPACITY_EXCEEDED 예외를 던진다")
    void registerSection_exceedsParentCapacity() {
        WarehouseSection parent = WarehouseSection.register(1L, null, "A", "A구역", "ZONE", BigDecimal.valueOf(500));
        WarehouseSectionRegisterCommand command =
                new WarehouseSectionRegisterCommand(1L, 10L, "A-02", "A-2랙", "RACK", BigDecimal.valueOf(101));
        when(warehouseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(activeWarehouse));
        when(warehouseSectionRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(warehouseSectionRepository.existsByWarehouseIdAndSectionCode(1L, "A-02")).thenReturn(false);
        when(warehouseSectionRepository.sumActiveCapacity(1L, 10L, null)).thenReturn(BigDecimal.valueOf(400));

        assertThatThrownBy(() -> warehouseSectionService.registerSection(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(WarehouseErrorCode.PARENT_CAPACITY_EXCEEDED.name());
    }

    @Test
    @DisplayName("창고 전체 수용량이 0이면 0보다 큰 구역을 등록할 수 없다")
    void registerSection_warehouseTotalCapacityZero() {
        Warehouse zeroWarehouse = Warehouse.register("WH-003", "미설정 창고", "서울시", null, BigDecimal.ZERO);
        WarehouseSectionRegisterCommand command =
                new WarehouseSectionRegisterCommand(3L, null, "A-01", "1구역", "ZONE", BigDecimal.ONE);
        when(warehouseRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(zeroWarehouse));
        when(warehouseSectionRepository.existsByWarehouseIdAndSectionCode(3L, "A-01")).thenReturn(false);
        when(warehouseSectionRepository.sumActiveCapacity(3L, null, null)).thenReturn(BigDecimal.ZERO);

        assertThatThrownBy(() -> warehouseSectionService.registerSection(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(WarehouseErrorCode.PARENT_CAPACITY_EXCEEDED.name());
    }

    @Test
    @DisplayName("수용량을 활성 하위 구역 합보다 작게 변경하면 CAPACITY_BELOW_CHILDREN 예외를 던진다")
    void updateSection_capacityBelowChildren() {
        WarehouseSection existing =
                WarehouseSection.register(1L, null, "A-01", "1구역", "ZONE", BigDecimal.valueOf(100));
        WarehouseSectionUpdateCommand command = new WarehouseSectionUpdateCommand(null, null, null, BigDecimal.valueOf(50));
        when(warehouseSectionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existing));
        when(warehouseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(activeWarehouse));
        when(warehouseSectionRepository.sumActiveCapacity(1L, 1L, null)).thenReturn(BigDecimal.valueOf(60));

        assertThatThrownBy(() -> warehouseSectionService.updateSection(1L, command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(WarehouseErrorCode.CAPACITY_BELOW_CHILDREN.name());
        verify(warehouseSectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("수용량을 늘려 형제 합이 상위 구역 수용량을 넘으면 PARENT_CAPACITY_EXCEEDED 예외를 던진다")
    void updateSection_increaseExceedsParentCapacity() {
        WarehouseSection parent = WarehouseSection.register(1L, null, "A", "A구역", "ZONE", BigDecimal.valueOf(500));
        WarehouseSection child = WarehouseSection.register(1L, 10L, "A-01", "A-1랙", "RACK", BigDecimal.valueOf(50));
        WarehouseSectionUpdateCommand command = new WarehouseSectionUpdateCommand(null, null, null, BigDecimal.valueOf(200));
        when(warehouseSectionRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(child));
        when(warehouseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(activeWarehouse));
        when(warehouseSectionRepository.sumActiveCapacity(1L, 11L, null)).thenReturn(BigDecimal.ZERO);
        when(warehouseSectionRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(warehouseSectionRepository.sumActiveCapacity(1L, 10L, 11L)).thenReturn(BigDecimal.valueOf(400));

        assertThatThrownBy(() -> warehouseSectionService.updateSection(11L, command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(WarehouseErrorCode.PARENT_CAPACITY_EXCEEDED.name());
        verify(warehouseSectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("수용량을 줄이는 변경은 상위 한도를 다시 확인하지 않는다")
    void updateSection_decrease_skipsUpperCheck() {
        WarehouseSection existing =
                WarehouseSection.register(1L, null, "A-01", "1구역", "ZONE", BigDecimal.valueOf(100));
        WarehouseSectionUpdateCommand command = new WarehouseSectionUpdateCommand(null, null, null, BigDecimal.valueOf(80));
        when(warehouseSectionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existing));
        when(warehouseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(activeWarehouse));
        when(warehouseSectionRepository.sumActiveCapacity(1L, 1L, null)).thenReturn(BigDecimal.ZERO);
        when(warehouseSectionRepository.save(any(WarehouseSection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(warehouseSectionService.updateSection(1L, command).getCapacity()).isEqualByComparingTo("80");
        verify(warehouseSectionRepository, never()).sumActiveCapacity(1L, null, 1L);
    }

    @Test
    @DisplayName("재활성화하면 형제 합이 상위 구역 수용량을 넘는 경우 PARENT_CAPACITY_EXCEEDED 예외를 던진다")
    void activateSection_exceedsParentCapacity() {
        WarehouseSection parent = WarehouseSection.register(1L, null, "A", "A구역", "ZONE", BigDecimal.valueOf(500));
        WarehouseSection child = WarehouseSection.register(1L, 10L, "A-01", "A-1랙", "RACK", BigDecimal.valueOf(50));
        child.deactivate();
        when(warehouseSectionRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(child));
        when(warehouseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(activeWarehouse));
        when(warehouseSectionRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(warehouseSectionRepository.sumActiveCapacity(1L, 10L, null)).thenReturn(BigDecimal.valueOf(480));

        assertThatThrownBy(() -> warehouseSectionService.activateSection(11L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(WarehouseErrorCode.PARENT_CAPACITY_EXCEEDED.name());
        verify(warehouseSectionRepository, never()).save(any());
    }

    private final AuthenticatedUser manager =
            new AuthenticatedUser(2L, UserRole.WAREHOUSE_MANAGER, List.of(1L), List.of());
    private final AuthenticatedUser hqAdmin = new AuthenticatedUser(1L, UserRole.HQ_ADMIN, List.of(), List.of());

    @Test
    @DisplayName("구역 단건은 구역이 속한 창고가 담당 창고일 때만 창고 관리자에게 보인다")
    void getSection_withActor_checksOwningWarehouse() {
        WarehouseSection mine = WarehouseSection.register(1L, null, "A-01", "1구역", "ZONE", BigDecimal.TEN);
        WarehouseSection other = WarehouseSection.register(2L, null, "B-01", "2구역", "ZONE", BigDecimal.TEN);
        when(warehouseSectionRepository.findById(10L)).thenReturn(Optional.of(mine));
        when(warehouseSectionRepository.findById(20L)).thenReturn(Optional.of(other));

        assertThat(warehouseSectionService.getSection(10L, manager)).isSameAs(mine);
        assertThat(warehouseSectionService.getSection(20L, hqAdmin)).isSameAs(other);
        assertThatThrownBy(() -> warehouseSectionService.getSection(20L, manager))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("구역 목록은 지정한 창고가 담당 창고일 때만 창고 관리자에게 보이고, 창고를 지정하지 않으면 본사만 가능하다")
    void getSections_withActor_checksWarehouse() {
        WarehouseSectionSearchCondition mine = new WarehouseSectionSearchCondition(1L, null, null, null, null);
        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(warehouseSectionRepository.search(mine)).thenReturn(List.of());

        assertThat(warehouseSectionService.getSections(mine, manager)).isEmpty();
        assertThatThrownBy(() -> warehouseSectionService.getSections(new WarehouseSectionSearchCondition(2L, null, null, null, null), manager))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
        assertThatThrownBy(() -> warehouseSectionService.getSections(new WarehouseSectionSearchCondition(null, null, null, null, null), manager))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
        verify(warehouseSectionRepository, never()).search(new WarehouseSectionSearchCondition(2L, null, null, null, null));
    }
}
