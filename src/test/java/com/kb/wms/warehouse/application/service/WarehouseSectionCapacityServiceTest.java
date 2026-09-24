package com.kb.wms.warehouse.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.warehouse.application.port.out.WarehouseRepository;
import com.kb.wms.warehouse.application.port.out.WarehouseSectionRepository;
import com.kb.wms.warehouse.domain.entity.Warehouse;
import com.kb.wms.warehouse.domain.entity.WarehouseSection;
import com.kb.wms.warehouse.exception.WarehouseErrorCode;

@ExtendWith(MockitoExtension.class)
class WarehouseSectionCapacityServiceTest {

    @Mock
    private WarehouseSectionRepository warehouseSectionRepository;
    @Mock
    private WarehouseRepository warehouseRepository;

    @InjectMocks
    private WarehouseSectionCapacityService warehouseSectionCapacityService;

    private static WarehouseSection section(BigDecimal capacity) {
        return WarehouseSection.register(1L, null, "A-01", "1구역", "ZONE", capacity);
    }

    private static Warehouse warehouse() {
        return Warehouse.register("WH-001", "서울 물류센터", "서울시 강남구", null, BigDecimal.TEN);
    }

    @Test
    @DisplayName("활성 창고의 활성 구역에 수용량 이내로 적치하면 사용 용량이 늘어난다")
    void occupy_success() {
        WarehouseSection section = section(BigDecimal.valueOf(100));
        when(warehouseSectionRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(section));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse()));

        warehouseSectionCapacityService.occupy(10L, 30L);

        assertThat(section.getCurrentCapacity()).isEqualByComparingTo("30");
        verify(warehouseSectionRepository).save(section);
    }

    @Test
    @DisplayName("비활성 구역에 적치하면 SECTION_INACTIVE 예외를 던진다")
    void occupy_inactiveSection() {
        WarehouseSection section = section(BigDecimal.valueOf(100));
        section.deactivate();
        when(warehouseSectionRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(section));

        assertThatThrownBy(() -> warehouseSectionCapacityService.occupy(10L, 30L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(WarehouseErrorCode.SECTION_INACTIVE.name());
        verify(warehouseSectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("비활성 창고의 구역에 적치하면 WAREHOUSE_INACTIVE 예외를 던진다")
    void occupy_inactiveWarehouse() {
        Warehouse inactive = warehouse();
        inactive.deactivate();
        when(warehouseSectionRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(section(BigDecimal.valueOf(100))));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> warehouseSectionCapacityService.occupy(10L, 30L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(WarehouseErrorCode.WAREHOUSE_INACTIVE.name());
    }

    @Test
    @DisplayName("수용량을 넘겨 적치하면 SECTION_CAPACITY_EXCEEDED 예외를 던진다")
    void occupy_capacityExceeded() {
        when(warehouseSectionRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(section(BigDecimal.valueOf(20))));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse()));

        assertThatThrownBy(() -> warehouseSectionCapacityService.occupy(10L, 30L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(WarehouseErrorCode.SECTION_CAPACITY_EXCEEDED.name());
    }

    @Test
    @DisplayName("여러 구역은 중복을 제거하고 section_id 오름차순으로 잠근다")
    void lock_sortedAndDistinct() {
        when(warehouseSectionRepository.findByIdForUpdate(any())).thenReturn(Optional.of(section(BigDecimal.TEN)));

        warehouseSectionCapacityService.lock(List.of(30L, 10L, 30L, 20L));

        InOrder inOrder = inOrder(warehouseSectionRepository);
        inOrder.verify(warehouseSectionRepository).findByIdForUpdate(10L);
        inOrder.verify(warehouseSectionRepository).findByIdForUpdate(20L);
        inOrder.verify(warehouseSectionRepository).findByIdForUpdate(30L);
        inOrder.verifyNoMoreInteractions();
    }
}
