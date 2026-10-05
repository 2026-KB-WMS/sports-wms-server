package com.kb.wms.store.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.common.statushistory.application.port.in.StatusHistoryUseCase;
import com.kb.wms.common.statushistory.domain.entity.StatusHistory;
import com.kb.wms.common.statushistory.domain.enums.StatusHistoryEntityType;
import com.kb.wms.store.application.port.in.StoreUseCase;
import com.kb.wms.store.application.port.in.command.StoreRegisterCommand;
import com.kb.wms.store.application.port.in.command.StoreUpdateCommand;
import com.kb.wms.store.application.port.in.query.StoreSearchCondition;
import com.kb.wms.store.application.port.in.result.StoreMembershipSummary;
import com.kb.wms.store.application.port.out.StoreMemberRepository;
import com.kb.wms.store.application.port.out.StoreOrderPresencePort;
import com.kb.wms.store.application.port.out.StoreRepository;
import com.kb.wms.store.domain.entity.Store;
import com.kb.wms.store.domain.entity.StoreMember;
import com.kb.wms.store.domain.enums.StoreStatus;
import com.kb.wms.store.exception.StoreErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService implements StoreUseCase {

    private final StoreRepository storeRepository;
    private final StoreMemberRepository storeMemberRepository;
    private final StoreOrderPresencePort storeOrderPresencePort;
    private final StatusHistoryUseCase statusHistoryUseCase;

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
     * 진행 중인 지점 발주(REQUESTED·APPROVED·ASSIGNED·ON_HOLD)가 있으면 비활성화할 수 없다(409 STORE_IN_USE).
     * 종결된 발주(COMPLETED·CANCELED·REJECTED)는 막지 않는다.
     * 비활성화 사유(선택, 최대 500자)는 StatusHistory.reason에 기록한다.
     */
    @Override
    @Transactional
    public Store deactivateStore(Long storeId, String reason, Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "요청 사용자는 필수입니다.");
        }
        if (reason != null && reason.strip().length() > StatusHistory.MAX_REASON_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "사유는 " + StatusHistory.MAX_REASON_LENGTH + "자 이하여야 합니다.");
        }
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new BusinessException(StoreErrorCode.STORE_NOT_FOUND));
        if (!store.isActive()) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 비활성화된 지점입니다.");
        }
        if (storeOrderPresencePort.hasInProgressOrders(storeId)) {
            throw new BusinessException(StoreErrorCode.STORE_IN_USE);
        }
        store.deactivate();
        Store saved = storeRepository.save(store);
        statusHistoryUseCase.record(StatusHistoryEntityType.STORE, storeId,
                StoreStatus.ACTIVE.name(), StoreStatus.INACTIVE.name(), reason, userId);
        return saved;
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
