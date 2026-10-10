package com.kb.wms.warehouse.application.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.warehouse.application.port.in.WarehouseSectionUseCase;
import com.kb.wms.warehouse.application.port.in.command.WarehouseSectionRegisterCommand;
import com.kb.wms.warehouse.application.port.in.command.WarehouseSectionUpdateCommand;
import com.kb.wms.warehouse.application.port.in.query.WarehouseSectionSearchCondition;
import com.kb.wms.warehouse.application.port.out.StockPresencePort;
import com.kb.wms.warehouse.application.port.out.WarehouseRepository;
import com.kb.wms.warehouse.application.port.out.WarehouseSectionRepository;
import com.kb.wms.warehouse.application.port.out.WarehouseUsagePort;
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
    private final StockPresencePort stockPresencePort;
    private final WarehouseUsagePort warehouseUsagePort;

    @Override
    @Transactional
    public WarehouseSection registerSection(WarehouseSectionRegisterCommand command) {
        if (!WarehouseSectionType.isValidCode(command.sectionType())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "허용되지 않은 구역 유형입니다.");
        }

        // 같은 상위 아래 구역들의 수용량 합을 비교하므로, 동시 등록과 겹치지 않게 창고 행을 잠근다.
        Warehouse warehouse = warehouseRepository.findByIdForUpdate(command.warehouseId())
                .orElseThrow(() -> new BusinessException(WarehouseErrorCode.WAREHOUSE_NOT_FOUND));
        if (!warehouse.isActive()) {
            throw new BusinessException(ErrorCode.CONFLICT, "비활성 창고에는 구역을 등록할 수 없습니다.");
        }

        BigDecimal upperCapacity = warehouse.getTotalCapacity();
        if (command.parentSectionId() != null) {
            WarehouseSection parent = warehouseSectionRepository.findById(command.parentSectionId())
                    .orElseThrow(() -> new BusinessException(WarehouseErrorCode.PARENT_SECTION_NOT_FOUND));
            if (!parent.getWarehouseId().equals(command.warehouseId())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "상위 구역은 같은 창고에 속해야 합니다.");
            }
            if (!parent.isActive()) {
                throw new BusinessException(ErrorCode.CONFLICT, "비활성 상위 구역에는 하위 구역을 등록할 수 없습니다.");
            }
            upperCapacity = parent.getCapacity();
        }

        if (warehouseSectionRepository.existsByWarehouseIdAndSectionCode(command.warehouseId(), command.sectionCode())) {
            throw new BusinessException(WarehouseErrorCode.DUPLICATE_SECTION_CODE);
        }
        requireWithinUpperCapacity(command.warehouseId(), command.parentSectionId(), null,
                upperCapacity, command.capacity());

        WarehouseSection section = WarehouseSection.register(
                command.warehouseId(), command.parentSectionId(), command.sectionCode(),
                command.name(), command.sectionType(), command.capacity());
        return warehouseSectionRepository.save(section);
    }

    @Override
    public List<WarehouseSection> getSections(WarehouseSectionSearchCondition condition) {
        if (condition.warehouseId() != null && !warehouseRepository.existsById(condition.warehouseId())) {
            throw new BusinessException(WarehouseErrorCode.WAREHOUSE_NOT_FOUND);
        }
        if (condition.parentSectionId() != null
                && warehouseSectionRepository.findById(condition.parentSectionId()).isEmpty()) {
            throw new BusinessException(WarehouseErrorCode.PARENT_SECTION_NOT_FOUND);
        }
        if (condition.sectionType() != null && !WarehouseSectionType.isValidCode(condition.sectionType())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "허용되지 않은 구역 유형입니다.");
        }
        return warehouseSectionRepository.search(condition);
    }

    @Override
    public List<WarehouseSection> getSections(WarehouseSectionSearchCondition condition,
                                              AuthenticatedUser actor) {
        if (condition.warehouseId() != null) {
            actor.requireWarehouseAccess(condition.warehouseId());
        } else if (!actor.isHqAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return getSections(condition);
    }

    @Override
    public WarehouseSection getSection(Long sectionId, AuthenticatedUser actor) {
        WarehouseSection section = getSection(sectionId);
        actor.requireWarehouseAccess(section.getWarehouseId());
        return section;
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

        // 수용량 축소는 현재 사용량과 비교하므로, 적치와 겹치지 않게 구역 행을 잠근 뒤 확인한다.
        WarehouseSection section = warehouseSectionRepository.findByIdForUpdate(sectionId)
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
            // 하위·형제 구역 합과 비교하므로 창고 행을 잠근 뒤 확인한다(구역 → 창고 순서, 등록과 같다).
            Warehouse warehouse = warehouseRepository.findByIdForUpdate(section.getWarehouseId())
                    .orElseThrow(() -> new BusinessException(WarehouseErrorCode.WAREHOUSE_NOT_FOUND));
            if (command.capacity().compareTo(warehouseSectionRepository.sumActiveCapacity(
                    section.getWarehouseId(), sectionId, null)) < 0) {
                throw new BusinessException(WarehouseErrorCode.CAPACITY_BELOW_CHILDREN);
            }
            // 줄이는 변경은 상위 한도를 넘을 수 없으므로 늘릴 때, 활성 구역만 확인한다.
            if (section.isActive() && command.capacity().compareTo(section.getCapacity()) > 0) {
                requireWithinUpperCapacity(section.getWarehouseId(), section.getParentSectionId(), sectionId,
                        upperCapacityOf(warehouse, section.getParentSectionId()), command.capacity());
            }
            section.changeCapacity(command.capacity());
        }

        return warehouseSectionRepository.save(section);
    }

    @Override
    @Transactional
    public WarehouseSection deactivateSection(Long sectionId) {
        // 구역 행을 잠가, 확인과 상태 변경 사이에 입고 적치(구역 행을 먼저 잠금)가 끼어들지 못하게 한다.
        WarehouseSection section = warehouseSectionRepository.findByIdForUpdate(sectionId)
                .orElseThrow(() -> new BusinessException(WarehouseErrorCode.SECTION_NOT_FOUND));
        if (!section.isActive()) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 비활성화된 구역입니다.");
        }
        if (stockPresencePort.hasStockInSection(sectionId)) {
            throw new BusinessException(WarehouseErrorCode.SECTION_HAS_INVENTORY);
        }
        if (warehouseSectionRepository.existsActiveChild(sectionId)) {
            throw new BusinessException(WarehouseErrorCode.SECTION_HAS_CHILDREN);
        }
        section.deactivate();
        return warehouseSectionRepository.save(section);
    }

    @Override
    @Transactional
    public void deleteSection(Long sectionId) {
        // 구역 행을 잠가, 확인과 삭제 사이에 입고 적치(구역 행을 먼저 잠금)가 끼어들지 못하게 한다.
        warehouseSectionRepository.findByIdForUpdate(sectionId)
                .orElseThrow(() -> new BusinessException(WarehouseErrorCode.SECTION_NOT_FOUND));
        if (stockPresencePort.hasStockInSection(sectionId)) {
            throw new BusinessException(WarehouseErrorCode.SECTION_HAS_INVENTORY,
                    "재고가 남아 있는 구역은 삭제할 수 없습니다.");
        }
        if (warehouseSectionRepository.existsChild(sectionId)) {
            throw new BusinessException(WarehouseErrorCode.SECTION_HAS_CHILDREN,
                    "하위 구역이 있어 삭제할 수 없습니다.");
        }
        if (warehouseUsagePort.isSectionReferenced(sectionId)) {
            throw new BusinessException(WarehouseErrorCode.SECTION_IN_USE);
        }
        warehouseSectionRepository.deleteById(sectionId);
    }

    @Override
    @Transactional
    public WarehouseSection activateSection(Long sectionId) {
        // 창고 비활성화가 구역 행을 잠그고 확인하므로, 같은 행을 잠가 서로 끼어들지 못하게 한다.
        WarehouseSection section = warehouseSectionRepository.findByIdForUpdate(sectionId)
                .orElseThrow(() -> new BusinessException(WarehouseErrorCode.SECTION_NOT_FOUND));
        if (section.isActive()) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 활성화된 구역입니다.");
        }
        Warehouse warehouse = warehouseRepository.findByIdForUpdate(section.getWarehouseId())
                .orElseThrow(() -> new BusinessException(WarehouseErrorCode.WAREHOUSE_NOT_FOUND));
        if (!warehouse.isActive()) {
            throw new BusinessException(ErrorCode.CONFLICT, "비활성 창고의 구역은 활성화할 수 없습니다.");
        }
        BigDecimal upperCapacity = warehouse.getTotalCapacity();
        if (section.getParentSectionId() != null) {
            WarehouseSection parent = warehouseSectionRepository.findById(section.getParentSectionId())
                    .orElseThrow(() -> new BusinessException(WarehouseErrorCode.PARENT_SECTION_NOT_FOUND));
            if (!parent.isActive()) {
                throw new BusinessException(ErrorCode.CONFLICT, "비활성 상위 구역의 하위 구역은 활성화할 수 없습니다.");
            }
            upperCapacity = parent.getCapacity();
        }
        // 비활성 구역은 합계에 들어 있지 않으므로, 활성화하면 형제 합에 자기 수용량이 더해진다.
        requireWithinUpperCapacity(section.getWarehouseId(), section.getParentSectionId(), null,
                upperCapacity, section.getCapacity());
        section.activate();
        return warehouseSectionRepository.save(section);
    }

    private BigDecimal upperCapacityOf(Warehouse warehouse, Long parentSectionId) {
        if (parentSectionId == null) {
            return warehouse.getTotalCapacity();
        }
        return warehouseSectionRepository.findById(parentSectionId)
                .orElseThrow(() -> new BusinessException(WarehouseErrorCode.PARENT_SECTION_NOT_FOUND))
                .getCapacity();
    }

    /** 같은 상위 아래 활성 구역들의 수용량 합에 capacity를 더해도 상위 수용량(0이면 0)을 넘지 않아야 한다. */
    private void requireWithinUpperCapacity(Long warehouseId, Long parentSectionId, Long excludeSectionId,
                                            BigDecimal upperCapacity, BigDecimal capacity) {
        BigDecimal siblings = warehouseSectionRepository.sumActiveCapacity(
                warehouseId, parentSectionId, excludeSectionId);
        if (siblings.add(capacity).compareTo(upperCapacity) > 0) {
            throw new BusinessException(WarehouseErrorCode.PARENT_CAPACITY_EXCEEDED);
        }
    }
}
