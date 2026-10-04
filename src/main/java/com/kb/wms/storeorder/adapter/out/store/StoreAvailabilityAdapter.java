package com.kb.wms.storeorder.adapter.out.store;

import org.springframework.stereotype.Component;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.storeorder.application.port.out.StoreAvailabilityPort;
import com.kb.wms.store.application.port.in.StoreUseCase;

import lombok.RequiredArgsConstructor;

/**
 * 지점 발주 도메인의 지점 상태 포트를 지점 도메인 유스케이스에 연결한다.
 */
@Component
@RequiredArgsConstructor
public class StoreAvailabilityAdapter implements StoreAvailabilityPort {

    private final StoreUseCase storeUseCase;

    @Override
    public void requireExists(Long storeId) {
        storeUseCase.getStore(storeId);
    }

    @Override
    public void requireActive(Long storeId) {
        if (!storeUseCase.getStore(storeId).isActive()) {
            throw new BusinessException(ErrorCode.CONFLICT, "비활성 지점의 발주는 등록하거나 승인할 수 없습니다.");
        }
    }
}
