package com.kb.wms.storeorder.application.service;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.storeorder.application.port.in.StoreOrderPresenceUseCase;
import com.kb.wms.storeorder.application.port.out.StoreOrderRepository;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreOrderPresenceService implements StoreOrderPresenceUseCase {

    /** 종결되지 않은 발주 상태. COMPLETED·CANCELED·REJECTED는 지점 비활성화를 막지 않는다. */
    private static final Set<StoreOrderStatus> IN_PROGRESS_STATUSES = Set.of(
            StoreOrderStatus.REQUESTED, StoreOrderStatus.APPROVED,
            StoreOrderStatus.ASSIGNED, StoreOrderStatus.ON_HOLD);

    private final StoreOrderRepository storeOrderRepository;

    @Override
    public boolean hasInProgressOrders(Long storeId) {
        return storeOrderRepository.existsByStoreIdAndStatusIn(storeId, IN_PROGRESS_STATUSES);
    }
}
