package com.kb.wms.store.adapter.out.storeorder;

import org.springframework.stereotype.Component;

import com.kb.wms.store.application.port.out.StoreOrderPresencePort;
import com.kb.wms.storeorder.application.port.in.StoreOrderPresenceUseCase;

import lombok.RequiredArgsConstructor;

/**
 * 지점 도메인의 발주 존재 확인 포트를 지점 발주 도메인 조회 유스케이스에 연결한다.
 */
@Component
@RequiredArgsConstructor
public class StoreOrderPresenceAdapter implements StoreOrderPresencePort {

    private final StoreOrderPresenceUseCase storeOrderPresenceUseCase;

    @Override
    public boolean hasInProgressOrders(Long storeId) {
        return storeOrderPresenceUseCase.hasInProgressOrders(storeId);
    }
}
