package com.kb.wms.warehouse.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.warehouse.application.port.in.WarehouseSectionUseCase;
import com.kb.wms.warehouse.application.port.in.command.WarehouseSectionRegisterCommand;
import com.kb.wms.warehouse.application.port.in.command.WarehouseSectionUpdateCommand;
import com.kb.wms.warehouse.application.port.out.WarehouseRepository;
import com.kb.wms.warehouse.application.port.out.WarehouseSectionRepository;
import com.kb.wms.warehouse.domain.entity.Warehouse;
import com.kb.wms.warehouse.domain.entity.WarehouseSection;
import com.kb.wms.warehouse.domain.enums.WarehouseSectionType;
import com.kb.wms.warehouse.exception.WarehouseErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WarehouseSectionService implements WarehouseSectionUseCase {

    private final WarehouseSectionRepository warehouseSectionRepository;
    private final WarehouseRepository warehouseRepository;

    @Override
    @Transactional
    public WarehouseSection registerSection(WarehouseSectionRegisterCommand command) {
        if (!WarehouseSectionType.isValidCode(command.sectionType())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "허용되지 않은 구역 유형입니다.");
        }

        Warehouse warehouse = warehouseRepository.findById(command.warehouseId())
                .orElseThrow(() -> new BusinessException(WarehouseErrorCode.WAREHOUSE_NOT_FOUND));
        if (!warehouse.isActive()) {
            throw new BusinessException(ErrorCode.CONFLICT, "비활성 창고에는 구역을 등록할 수 없습니다.");
        }

        if (command.parentSectionId() != null) {
            WarehouseSection parent = warehouseSectionRepository.findById(command.parentSectionId())
                    .orElseThrow(() -> new BusinessException(WarehouseErrorCode.PARENT_SECTION_NOT_FOUND));
            if (!parent.getWarehouseId().equals(command.warehouseId())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "상위 구역은 같은 창고에 속해야 합니다.");
            }
            if (!parent.isActive()) {
                throw new BusinessException(ErrorCode.CONFLICT, "비활성 상위 구역에는 하위 구역을 등록할 수 없습니다.");
            }
        }

        if (warehouseSectionRepository.existsByWarehouseIdAndSectionCode(command.warehouseId(), command.sectionCode())) {
            throw new BusinessException(WarehouseErrorCode.DUPLICATE_SECTION_CODE);
        }

        WarehouseSection section = WarehouseSection.register(
                command.warehouseId(), command.parentSectionId(), command.sectionCode(),
                command.name(), command.sectionType(), command.capacity());
        return warehouseSectionRepository.save(section);
    }

    @Override
    public List<WarehouseSection> getSections(Long warehouseId) {
        return warehouseSectionRepository.findAll(warehouseId);
    }

    @Override
    public WarehouseSection getSection(Long sectionId) {
        return warehouseSectionRepository.findById(sectionId)
                .orElseThrow(() -> new BusinessException(WarehouseErrorCode.SECTION_NOT_FOUND));
    }

    @Override
    @Transactional
    public WarehouseSection updateSection(Long sectionId, WarehouseSectionUpdateCommand command) {
        if (command.hasNoChanges()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "수정할 필드를 하나 이상 입력해주세요.");
        }

        WarehouseSection section = warehouseSectionRepository.findById(sectionId)
                .orElseThrow(() -> new BusinessException(WarehouseErrorCode.SECTION_NOT_FOUND));

        if (command.sectionCode() != null && !command.sectionCode().equals(section.getSectionCode())) {
            if (warehouseSectionRepository.existsByWarehouseIdAndSectionCode(
                    section.getWarehouseId(), command.sectionCode())) {
                throw new BusinessException(WarehouseErrorCode.DUPLICATE_SECTION_CODE);
            }
            section.changeSectionCode(command.sectionCode());
        }
        if (command.name() != null) {
            section.changeName(command.name());
        }
        if (command.sectionType() != null) {
            if (!WarehouseSectionType.isValidCode(command.sectionType())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "허용되지 않은 구역 유형입니다.");
            }
            section.changeSectionType(command.sectionType());
        }
        if (command.capacity() != null) {
            if (command.capacity().compareTo(section.getCurrentCapacity()) < 0) {
                throw new BusinessException(WarehouseErrorCode.CAPACITY_BELOW_USAGE);
            }
            section.changeCapacity(command.capacity());
        }

        return warehouseSectionRepository.save(section);
    }

    @Override
    @Transactional
    public WarehouseSection deactivateSection(Long sectionId) {
        WarehouseSection section = warehouseSectionRepository.findById(sectionId)
                .orElseThrow(() -> new BusinessException(WarehouseErrorCode.SECTION_NOT_FOUND));
        if (!section.isActive()) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 비활성화된 구역입니다.");
        }
        section.deactivate();
        return warehouseSectionRepository.save(section);
    }
}
