package com.kb.wms.auth.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.auth.application.port.in.UserManagementUseCase;
import com.kb.wms.auth.application.port.in.UserUseCase;
import com.kb.wms.auth.application.port.in.command.UserUpdateCommand;
import com.kb.wms.auth.application.port.out.StoreOwnerAssignPort;
import com.kb.wms.auth.application.port.out.WarehouseManagerAssignPort;
import com.kb.wms.auth.domain.entity.User;

import lombok.RequiredArgsConstructor;

/**
 * 승인과 소속 배정을 한 트랜잭션에서 처리한다(ADR-022). 배정 서비스가 사용자 조회를 위해 UserUseCase를
 * 쓰므로, UserService가 배정 서비스를 직접 부르면 순환 의존이 생겨 이 서비스가 둘을 이어 준다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserManagementService implements UserManagementUseCase {

    private final UserUseCase userUseCase;
    private final WarehouseManagerAssignPort warehouseManagerAssignPort;
    private final StoreOwnerAssignPort storeOwnerAssignPort;

    @Override
    public User updateUser(UserUpdateCommand command) {
        // 상태를 ACTIVE로 바꾼 뒤 배정해야 배정 서비스의 "활성 사용자만" 검사를 통과한다.
        User user = userUseCase.updateUser(command);
        if (command.warehouseId() != null) {
            warehouseManagerAssignPort.assignManager(command.warehouseId(), user.getUserId());
        } else if (command.storeId() != null) {
            storeOwnerAssignPort.assignOwner(command.storeId(), user.getUserId());
        }
        return user;
    }
}
