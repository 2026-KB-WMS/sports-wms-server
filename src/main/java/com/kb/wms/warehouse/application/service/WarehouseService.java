package com.kb.wms.warehouse.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.common.locationcode.LocationCodeIssuer;
import com.kb.wms.common.locationcode.LocationType;
import com.kb.wms.common.locationcode.RegionCode;
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.warehouse.application.port.in.WarehouseSectionCapacityUseCase;
import com.kb.wms.warehouse.application.port.in.WarehouseUseCase;
import com.kb.wms.warehouse.application.port.in.command.WarehouseRegisterCommand;
import com.kb.wms.warehouse.application.port.in.command.WarehouseUpdateCommand;
import com.kb.wms.warehouse.application.port.in.query.WarehouseSearchCondition;
import com.kb.wms.warehouse.application.port.in.result.WarehouseMembershipSummary;
import com.kb.wms.warehouse.application.port.in.result.WarehouseView;
import com.kb.wms.warehouse.application.port.out.StockPresencePort;
import com.kb.wms.warehouse.application.port.out.WarehouseMemberRepository;
import com.kb.wms.warehouse.application.port.out.WarehouseRepository;
import com.kb.wms.warehouse.application.port.out.WarehouseSectionRepository;
import com.kb.wms.warehouse.application.port.out.WarehouseUsagePort;
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
    private final WarehouseUsagePort warehouseUsagePort;
    private final LocationCodeIssuer locationCodeIssuer;

    @Override
    @Transactional
    public Warehouse registerWarehouse(WarehouseRegisterCommand command) {
        String warehouseCode = issueCode(RegionCode.fromAddress(command.address()));
        Warehouse warehouse = Warehouse.register(
                warehouseCode, command.name(), command.address(),
                command.contactNumber(), command.totalCapacity());
        warehouse.changeCoordinates(command.latitude(), command.longitude());
        return warehouseRepository.save(warehouse);
    }

    private String issueCode(RegionCode region) {
        return locationCodeIssuer.issue(LocationType.WAREHOUSE, region, warehouseRepository::existsByWarehouseCode);
    }

    /**
     * 발급 형식의 코드(WH-SU-001)를 가진 창고가 다른 시·도로 주소를 바꾸면 새 시·도의 다음 번호로 코드를 다시 발급한다.
     * 같은 시·도 안의 주소 수정과 과거에 직접 입력된 코드는 바꾸지 않는다.
     */
    private void reissueCodeIfRegionChanged(Warehouse warehouse, RegionCode newRegion) {
        LocationCodeIssuer.regionOf(warehouse.getWarehouseCode())
                .filter(current -> current != newRegion)
                .ifPresent(current -> warehouse.reissueCode(issueCode(newRegion)));
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
    public WarehouseView getWarehouseView(Long warehouseId) {
        Warehouse warehouse = getWarehouse(warehouseId);
        return new WarehouseView(warehouse.getWarehouseId(), warehouse.getName(), warehouse.isActive());
    }

    @Override
    public Warehouse getWarehouse(Long warehouseId, AuthenticatedUser actor) {
        actor.requireWarehouseAccess(warehouseId);
        return getWarehouse(warehouseId);
    }

    @Override
    @Transactional
    public Warehouse updateWarehouse(Long warehouseId, WarehouseUpdateCommand command) {
        if (command.hasNoChanges()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "수정할 필드를 하나 이상 입력해주세요.");
        }

        // 최상위 구역 합과 비교하므로, 구역 등록·수정과 겹치지 않게 창고 행을 잠근 뒤 확인한다.
        Warehouse warehouse = warehouseRepository.findByIdForUpdate(warehouseId)
                .orElseThrow(() -> new BusinessException(WarehouseErrorCode.WAREHOUSE_NOT_FOUND));

        if (command.name() != null) {
            warehouse.changeName(command.name());
        }
        if (command.address() != null) {
            RegionCode region = RegionCode.fromAddress(command.address());
            warehouse.changeAddress(command.address());
            reissueCodeIfRegionChanged(warehouse, region);
        }
        if (command.contactNumber() != null) {
            warehouse.changeContactNumber(command.contactNumber());
        }
        if (command.latitude() != null && command.longitude() != null) {
            warehouse.changeCoordinates(command.latitude(), command.longitude());
        }
        if (command.totalCapacity() != null) {
            if (command.totalCapacity().compareTo(
                    warehouseSectionRepository.sumActiveCapacity(warehouseId, null, null)) < 0) {
                throw new BusinessException(WarehouseErrorCode.CAPACITY_BELOW_CHILDREN);
            }
            warehouse.changeTotalCapacity(command.totalCapacity());
        }

        return warehouseRepository.save(warehouse);
    }

    /**
     * 창고의 구역 행을 모두 잠근 뒤(section_id 오름차순, 입고 적치와 같은 잠금 순서) 재고 잔량을 확인한다.
     * 진행 중 업무(입고 ARRIVED·INSPECTING, 발주 REQUESTED·CONFIRMED, 출고 READY·PICKING·PICKED·SHIPPED,
     * 배정된 지점 발주 REQUESTED·APPROVED·ASSIGNED·ON_HOLD)가 하나라도 있으면 409 WAREHOUSE_IN_USE.
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
        if (stockPresencePort.hasStockInWarehouse(warehouseId)
                || warehouseUsagePort.hasInProgressInbounds(warehouseId)
                || warehouseUsagePort.hasInProgressPurchaseOrders(warehouseId)
                || warehouseUsagePort.hasInProgressOutbounds(warehouseId)
                || warehouseUsagePort.hasInProgressStoreOrders(warehouseId)) {
            throw new BusinessException(WarehouseErrorCode.WAREHOUSE_IN_USE);
        }
        warehouse.deactivate();
        return warehouseRepository.save(warehouse);
    }

    @Override
    @Transactional
    public Warehouse activateWarehouse(Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new BusinessException(WarehouseErrorCode.WAREHOUSE_NOT_FOUND));
        if (warehouse.isActive()) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 활성화된 창고입니다.");
        }
        warehouse.activate();
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
