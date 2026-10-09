package com.kb.wms.warehouse.application.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.auth.application.port.in.UserUseCase;
import com.kb.wms.auth.domain.entity.User;
import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.auth.domain.enums.UserStatus;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.warehouse.application.port.in.WarehouseMemberUseCase;
import com.kb.wms.warehouse.application.port.in.command.WarehouseMemberAssignCommand;
import com.kb.wms.warehouse.application.port.in.result.WarehouseMemberView;
import com.kb.wms.warehouse.application.port.out.WarehouseMemberRepository;
import com.kb.wms.warehouse.application.port.out.WarehouseRepository;
import com.kb.wms.warehouse.domain.entity.Warehouse;
import com.kb.wms.warehouse.domain.entity.WarehouseMember;
import com.kb.wms.warehouse.domain.enums.WarehouseManagementType;
import com.kb.wms.warehouse.exception.WarehouseErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WarehouseMemberService implements WarehouseMemberUseCase {

    private final WarehouseMemberRepository warehouseMemberRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserUseCase userUseCase;

    @Override
    @Transactional
    public WarehouseMemberView assignManager(WarehouseMemberAssignCommand command) {
        if (!WarehouseManagementType.isValidCode(command.memberRole())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "허용되지 않은 담당 역할입니다.");
        }

        Warehouse warehouse = warehouseRepository.findById(command.warehouseId())
                .orElseThrow(() -> new BusinessException(WarehouseErrorCode.WAREHOUSE_NOT_FOUND));
        if (!warehouse.isActive()) {
            throw new BusinessException(ErrorCode.CONFLICT, "비활성 창고에는 관리자를 배정할 수 없습니다.");
        }

        // 없는 사용자는 USER_NOT_FOUND(404). PENDING 사용자는 소속 배정 뒤에 승인하므로 배정할 수 있다.
        User user = userUseCase.getUser(command.userId());
        if (user.getRole() != UserRole.WAREHOUSE_MANAGER) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "창고 관리자로 배정할 수 있는 역할은 WAREHOUSE_MANAGER입니다.");
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.CONFLICT, "비활성 사용자에게는 창고 관리자를 배정할 수 없습니다.");
        }

        if (warehouseMemberRepository.existsByWarehouseIdAndUserId(command.warehouseId(), command.userId())) {
            throw new BusinessException(WarehouseErrorCode.ALREADY_ASSIGNED);
        }

        WarehouseMember member = WarehouseMember.assign(
                command.warehouseId(), command.userId(), command.memberRole(), LocalDateTime.now());
        return WarehouseMemberView.of(warehouseMemberRepository.save(member), user.getName(), user.getLoginId());
    }

    @Override
    public List<WarehouseMemberView> getManagers(Long warehouseId, Long userId, String keyword) {
        if (warehouseId != null && !warehouseRepository.existsById(warehouseId)) {
            throw new BusinessException(WarehouseErrorCode.WAREHOUSE_NOT_FOUND);
        }
        return warehouseMemberRepository.search(warehouseId, userId, keyword);
    }

    @Override
    @Transactional
    public void releaseManager(Long warehouseMemberId) {
        warehouseMemberRepository.findById(warehouseMemberId)
                .orElseThrow(() -> new BusinessException(WarehouseErrorCode.MEMBER_NOT_FOUND));
        warehouseMemberRepository.deleteById(warehouseMemberId);
    }
}
