package com.kb.wms.store.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.common.locationcode.LocationCodeIssuer;
import com.kb.wms.common.locationcode.LocationType;
import com.kb.wms.common.locationcode.RegionCode;
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.statushistory.application.port.in.StatusHistoryUseCase;
import com.kb.wms.statushistory.domain.entity.StatusHistory;
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
import com.kb.wms.store.domain.enums.StoreHistoryEntityType;
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
    private final LocationCodeIssuer locationCodeIssuer;

    @Override
    @Transactional
    public Store registerStore(StoreRegisterCommand command) {
        String storeCode = issueCode(RegionCode.fromAddress(command.address()));
        Store store = Store.register(
                storeCode, command.name(), command.address(),
                command.contactName(), command.contactNumber());
        store.changeCoordinates(command.latitude(), command.longitude());
        return storeRepository.save(store);
    }

    private String issueCode(RegionCode region) {
        return locationCodeIssuer.issue(LocationType.STORE, region, storeRepository::existsByStoreCode);
    }

    /**
     * 발급 형식의 코드(ST-SU-001)를 가진 지점이 다른 시·도로 주소를 바꾸면 새 시·도의 다음 번호로 코드를 다시 발급한다.
     * 같은 시·도 안의 주소 수정과 과거에 직접 입력된 코드는 바꾸지 않는다.
     */
    private void reissueCodeIfRegionChanged(Store store, RegionCode newRegion) {
        LocationCodeIssuer.regionOf(store.getStoreCode())
                .filter(current -> current != newRegion)
                .ifPresent(current -> store.reissueCode(issueCode(newRegion)));
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
    public Store getStore(Long storeId, AuthenticatedUser actor) {
        actor.requireStoreAccess(storeId);
        return getStore(storeId);
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
            RegionCode region = RegionCode.fromAddress(command.address());
            store.changeAddress(command.address());
            reissueCodeIfRegionChanged(store, region);
        }
        if (command.contactName() != null) {
            store.changeContactName(command.contactName());
        }
        if (command.contactNumber() != null) {
            store.changeContactNumber(command.contactNumber());
        }
        if (command.latitude() != null && command.longitude() != null) {
            store.changeCoordinates(command.latitude(), command.longitude());
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
        statusHistoryUseCase.record(StoreHistoryEntityType.STORE.name(), storeId,
                StoreStatus.ACTIVE.name(), StoreStatus.INACTIVE.name(), reason, userId);
        return saved;
    }

    @Override
    @Transactional
    public Store activateStore(Long storeId, Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "요청 사용자는 필수입니다.");
        }
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new BusinessException(StoreErrorCode.STORE_NOT_FOUND));
        if (store.isActive()) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 활성화된 지점입니다.");
        }
        store.activate();
        Store saved = storeRepository.save(store);
        statusHistoryUseCase.record(StoreHistoryEntityType.STORE.name(), storeId,
                StoreStatus.INACTIVE.name(), StoreStatus.ACTIVE.name(), null, userId);
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
