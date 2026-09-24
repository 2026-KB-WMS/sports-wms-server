package com.kb.wms.warehouse.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.warehouse.application.port.in.WarehouseSectionCapacityUseCase;
import com.kb.wms.warehouse.application.port.in.WarehouseUseCase;
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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WarehouseService implements WarehouseUseCase {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseMemberRepository warehouseMemberRepository;
    private final WarehouseSectionRepository warehouseSectionRepository;
    private final WarehouseSectionCapacityUseCase warehouseSectionCapacityUseCase;
    private final StockPresencePort stockPresencePort;

    @Override
    @Transactional
    public Warehouse registerWarehouse(WarehouseRegisterCommand command) {
        if (warehouseRepository.existsByWarehouseCode(command.warehouseCode())) {
            throw new BusinessException(WarehouseErrorCode.DUPLICATE_WAREHOUSE_CODE);
        }
        Warehouse warehouse = Warehouse.register(
                command.warehouseCode(), command.name(), command.address(),
                command.contactNumber(), command.totalCapacity());
        return warehouseRepository.save(warehouse);
    }

    @Override
    public List<Warehouse> getWarehouses(WarehouseSearchCondition condition) {
        return warehouseRepository.search(condition);
    }

    @Override
    public Warehouse getWarehouse(Long warehouseId) {
        return warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new BusinessException(WarehouseErrorCode.WAREHOUSE_NOT_FOUND));
    }

    @Override
    @Transactional
    public Warehouse updateWarehouse(Long warehouseId, WarehouseUpdateCommand command) {
        if (command.hasNoChanges()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "수정할 필드를 하나 이상 입력해주세요.");
        }

        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new BusinessException(WarehouseErrorCode.WAREHOUSE_NOT_FOUND));

        if (command.name() != null) {
            warehouse.changeName(command.name());
        }
        if (command.address() != null) {
            warehouse.changeAddress(command.address());
        }
        if (command.contactNumber() != null) {
            warehouse.changeContactNumber(command.contactNumber());
        }
        if (command.totalCapacity() != null) {
            warehouse.changeTotalCapacity(command.totalCapacity());
        }

        return warehouseRepository.save(warehouse);
    }

    /**
     * 창고의 구역 행을 모두 잠근 뒤(section_id 오름차순, 입고 적치와 같은 잠금 순서) 재고 잔량을 확인한다.
     * 진행 중인 입고·출고·발주 배정 검사는 해당 도메인이 구현되면 추가한다.
     */
    @Override
    @Transactional
    public Warehouse deactivateWarehouse(Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new BusinessException(WarehouseErrorCode.WAREHOUSE_NOT_FOUND));
        if (!warehouse.isActive()) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 비활성화된 창고입니다.");
        }
        warehouseSectionCapacityUseCase.lock(warehouseSectionRepository.findAllByWarehouseId(warehouseId).stream()
                .map(WarehouseSection::getSectionId)
                .toList());
        if (stockPresencePort.hasStockInWarehouse(warehouseId)) {
            throw new BusinessException(WarehouseErrorCode.WAREHOUSE_IN_USE);
        }
        warehouse.deactivate();
        return warehouseRepository.save(warehouse);
    }

    @Override
    public List<WarehouseMembershipSummary> getMyWarehouses(Long userId) {
        List<WarehouseMember> memberships = warehouseMemberRepository.findByUserId(userId);
        return memberships.stream()
                .map(member -> {
                    Warehouse warehouse = warehouseRepository.findById(member.getWarehouseId())
                            .orElseThrow(() -> new BusinessException(WarehouseErrorCode.WAREHOUSE_NOT_FOUND));
                    return new WarehouseMembershipSummary(
                            warehouse.getWarehouseId(), warehouse.getWarehouseCode(), warehouse.getName(),
                            warehouse.getAddress(), warehouse.getContactNumber(), warehouse.getTotalCapacity(),
                            warehouse.isActive(), member.getWarehouseMemberId(), member.getMemberRole(),
                            member.getAssignedAt());
                })
                .toList();
    }
}
