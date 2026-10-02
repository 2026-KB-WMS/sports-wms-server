package com.kb.wms.inbound.adapter.out.warehouse;

import org.springframework.stereotype.Component;

import com.kb.wms.inbound.application.port.out.InboundSectionPort;
import com.kb.wms.warehouse.application.port.in.WarehouseSectionUseCase;
import com.kb.wms.warehouse.domain.entity.WarehouseSection;

import lombok.RequiredArgsConstructor;

/**
 * 입고 도메인의 구역 포트를 창고 도메인 구역 유스케이스에 연결한다.
 */
@Component
@RequiredArgsConstructor
public class InboundSectionAdapter implements InboundSectionPort {

    private final WarehouseSectionUseCase warehouseSectionUseCase;

    @Override
    public SectionInfo getSection(Long sectionId) {
        WarehouseSection section = warehouseSectionUseCase.getSection(sectionId);
        return new SectionInfo(section.getSectionId(), section.getWarehouseId(),
                section.getSectionType(), section.isActive());
    }
}
