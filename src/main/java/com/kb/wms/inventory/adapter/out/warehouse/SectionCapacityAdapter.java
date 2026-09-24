package com.kb.wms.inventory.adapter.out.warehouse;

import org.springframework.stereotype.Component;

import com.kb.wms.inventory.application.port.out.SectionCapacityPort;
import com.kb.wms.warehouse.application.port.in.WarehouseSectionCapacityUseCase;

import lombok.RequiredArgsConstructor;

/**
 * 재고 도메인의 구역 용량 포트를 창고 도메인 유스케이스로 연결한다.
 */
@Component
@RequiredArgsConstructor
public class SectionCapacityAdapter implements SectionCapacityPort {

    private final WarehouseSectionCapacityUseCase warehouseSectionCapacityUseCase;

    @Override
    public void occupy(Long sectionId, long quantity) {
        warehouseSectionCapacityUseCase.occupy(sectionId, quantity);
    }

    @Override
    public void vacate(Long sectionId, long quantity) {
        warehouseSectionCapacityUseCase.vacate(sectionId, quantity);
    }
}
