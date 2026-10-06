package com.kb.wms.store.application.service;

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
import com.kb.wms.store.application.port.in.StoreMemberUseCase;
import com.kb.wms.store.application.port.in.command.StoreMemberAssignCommand;
import com.kb.wms.store.application.port.in.result.StoreMemberView;
import com.kb.wms.store.application.port.out.StoreMemberRepository;
import com.kb.wms.store.application.port.out.StoreRepository;
import com.kb.wms.store.domain.entity.Store;
import com.kb.wms.store.domain.entity.StoreMember;
import com.kb.wms.store.domain.enums.StoreManagementType;
import com.kb.wms.store.exception.StoreErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreMemberService implements StoreMemberUseCase {

    private final StoreMemberRepository storeMemberRepository;
    private final StoreRepository storeRepository;
    private final UserUseCase userUseCase;

    @Override
    @Transactional
    public StoreMemberView assignManager(StoreMemberAssignCommand command) {
        if (!StoreManagementType.isValidCode(command.memberRole())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "허용되지 않은 담당 역할입니다.");
        }

        Store store = storeRepository.findById(command.storeId())
                .orElseThrow(() -> new BusinessException(StoreErrorCode.STORE_NOT_FOUND));
        if (!store.isActive()) {
            throw new BusinessException(ErrorCode.CONFLICT, "비활성 지점에는 관리자를 배정할 수 없습니다.");
        }

        // 없는 사용자는 USER_NOT_FOUND(404). PENDING 사용자는 소속 배정 뒤에 승인하므로 배정할 수 있다.
        User user = userUseCase.getUser(command.userId());
        if (user.getRole() != UserRole.STORE_OWNER) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "지점 관리자로 배정할 수 있는 역할은 STORE_OWNER입니다.");
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.CONFLICT, "비활성 사용자에게는 지점 관리자를 배정할 수 없습니다.");
        }

        if (storeMemberRepository.existsByStoreIdAndUserId(command.storeId(), command.userId())) {
            throw new BusinessException(StoreErrorCode.ALREADY_ASSIGNED);
        }

        StoreMember member = StoreMember.assign(
                command.storeId(), command.userId(), command.memberRole(), LocalDateTime.now());
        return StoreMemberView.of(storeMemberRepository.save(member), user.getName(), user.getLoginId());
    }

    @Override
    public List<StoreMemberView> getManagers(Long storeId, Long userId, String keyword) {
        if (storeId != null && !storeRepository.existsById(storeId)) {
            throw new BusinessException(StoreErrorCode.STORE_NOT_FOUND);
        }
        return storeMemberRepository.search(storeId, userId, keyword);
    }

    @Override
    @Transactional
    public void releaseManager(Long storeMemberId) {
        storeMemberRepository.findById(storeMemberId)
                .orElseThrow(() -> new BusinessException(StoreErrorCode.MEMBER_NOT_FOUND));
        storeMemberRepository.deleteById(storeMemberId);
    }
}
