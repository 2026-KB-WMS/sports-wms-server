package com.kb.wms.warehouse.application.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.warehouse.application.port.in.WarehouseSectionCapacityUseCase;
import com.kb.wms.warehouse.application.port.out.WarehouseSectionRepository;
import com.kb.wms.warehouse.domain.entity.WarehouseSection;
import com.kb.wms.warehouse.exception.WarehouseErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class WarehouseSectionCapacityService implements WarehouseSectionCapacityUseCase {

    private final WarehouseSectionRepository warehouseSectionRepository;

    @Override
    public void occupy(Long sectionId, long quantity) {
        if (quantity <= 0) {
            return;
        }
        WarehouseSection section = lock(sectionId);
        BigDecimal amount = BigDecimal.valueOf(quantity);
        if (!section.canOccupy(amount)) {
            throw new BusinessException(WarehouseErrorCode.SECTION_CAPACITY_EXCEEDED,
                    "구역(" + section.getSectionCode() + ")의 수용량을 초과합니다. 남은 용량: "
                            + section.availableCapacity().stripTrailingZeros().toPlainString());
        }
        section.occupy(amount);
        warehouseSectionRepository.save(section);
    }

    @Override
    public void vacate(Long sectionId, long quantity) {
        if (quantity <= 0) {
            return;
        }
        WarehouseSection section = lock(sectionId);
        section.vacate(BigDecimal.valueOf(quantity));
        warehouseSectionRepository.save(section);
    }

    private WarehouseSection lock(Long sectionId) {
        return warehouseSectionRepository.findByIdForUpdate(sectionId)
                .orElseThrow(() -> new BusinessException(WarehouseErrorCode.SECTION_NOT_FOUND));
    }
}
