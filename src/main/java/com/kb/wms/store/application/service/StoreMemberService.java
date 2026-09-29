package com.kb.wms.store.application.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.store.application.port.in.StoreMemberUseCase;
import com.kb.wms.store.application.port.in.command.StoreMemberAssignCommand;
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

    /**
     * 대상 사용자가 STORE_OWNER 역할인지, 존재하는 userId인지는 User 도메인이 구현되면 검증을 추가한다.
     */
    @Override
    @Transactional
    public StoreMember assignManager(StoreMemberAssignCommand command) {
        if (!StoreManagementType.isValidCode(command.memberRole())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "허용되지 않은 담당 역할입니다.");
        }

        Store store = storeRepository.findById(command.storeId())
                .orElseThrow(() -> new BusinessException(StoreErrorCode.STORE_NOT_FOUND));
        if (!store.isActive()) {
            throw new BusinessException(ErrorCode.CONFLICT, "비활성 지점에는 관리자를 배정할 수 없습니다.");
        }

        if (storeMemberRepository.existsByStoreIdAndUserId(command.storeId(), command.userId())) {
            throw new BusinessException(StoreErrorCode.ALREADY_ASSIGNED);
        }

        StoreMember member = StoreMember.assign(
                command.storeId(), command.userId(), command.memberRole(), LocalDateTime.now());
        return storeMemberRepository.save(member);
    }

    @Override
    public List<StoreMember> getManagers(Long storeId, Long userId) {
        if (storeId != null && !storeRepository.existsById(storeId)) {
            throw new BusinessException(StoreErrorCode.STORE_NOT_FOUND);
        }
        return storeMemberRepository.findAll(storeId, userId);
    }

    @Override
    @Transactional
    public void releaseManager(Long storeMemberId) {
        storeMemberRepository.findById(storeMemberId)
                .orElseThrow(() -> new BusinessException(StoreErrorCode.MEMBER_NOT_FOUND));
        storeMemberRepository.deleteById(storeMemberId);
    }
}
