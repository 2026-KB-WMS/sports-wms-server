package com.kb.wms.store.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.store.application.port.in.StoreUseCase;
import com.kb.wms.store.application.port.in.command.StoreRegisterCommand;
import com.kb.wms.store.application.port.in.command.StoreUpdateCommand;
import com.kb.wms.store.application.port.in.query.StoreSearchCondition;
import com.kb.wms.store.application.port.in.result.StoreMembershipSummary;
import com.kb.wms.store.application.port.out.StoreMemberRepository;
import com.kb.wms.store.application.port.out.StoreRepository;
import com.kb.wms.store.domain.entity.Store;
import com.kb.wms.store.domain.entity.StoreMember;
import com.kb.wms.store.exception.StoreErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService implements StoreUseCase {

    private final StoreRepository storeRepository;
    private final StoreMemberRepository storeMemberRepository;

    @Override
    @Transactional
    public Store registerStore(StoreRegisterCommand command) {
        if (storeRepository.existsByStoreCode(command.storeCode())) {
            throw new BusinessException(StoreErrorCode.DUPLICATE_STORE_CODE);
        }
        Store store = Store.register(
                command.storeCode(), command.name(), command.address(),
                command.contactName(), command.contactNumber());
        return storeRepository.save(store);
    }

    @Override
    public List<Store> getStores(StoreSearchCondition condition) {
        return storeRepository.search(condition);
    }

    @Override
    public Store getStore(Long storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new BusinessException(StoreErrorCode.STORE_NOT_FOUND));
    }

    @Override
    @Transactional
    public Store updateStore(Long storeId, StoreUpdateCommand command) {
        if (command.hasNoChanges()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "수정할 필드를 하나 이상 입력해주세요.");
        }

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new BusinessException(StoreErrorCode.STORE_NOT_FOUND));

        if (command.name() != null) {
            store.changeName(command.name());
        }
        if (command.address() != null) {
            store.changeAddress(command.address());
        }
        if (command.contactName() != null) {
            store.changeContactName(command.contactName());
        }
        if (command.contactNumber() != null) {
            store.changeContactNumber(command.contactNumber());
        }

        return storeRepository.save(store);
    }

    /**
     * 진행 중인 지점 발주(REQUESTED·APPROVED·ASSIGNED·ON_HOLD) 검사(409 STORE_IN_USE)는
     * 지점 발주(storeorder) 도메인이 구현되면 추가한다.
     */
    @Override
    @Transactional
    public Store deactivateStore(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new BusinessException(StoreErrorCode.STORE_NOT_FOUND));
        if (!store.isActive()) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 비활성화된 지점입니다.");
        }
        store.deactivate();
        return storeRepository.save(store);
    }

    @Override
    public List<StoreMembershipSummary> getMyStores(Long userId) {
        List<StoreMember> memberships = storeMemberRepository.findByUserId(userId);
        return memberships.stream()
                .map(member -> {
                    Store store = storeRepository.findById(member.getStoreId())
                            .orElseThrow(() -> new BusinessException(StoreErrorCode.STORE_NOT_FOUND));
                    return new StoreMembershipSummary(
                            store.getStoreId(), store.getStoreCode(), store.getName(),
                            store.getAddress(), store.getContactName(), store.getContactNumber(),
                            store.isActive(), member.getStoreMemberId(), member.getMemberRole(),
                            member.getAssignedAt());
                })
                .toList();
    }
}
