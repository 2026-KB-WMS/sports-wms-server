package com.kb.wms.auth.adapter.out.store;

import org.springframework.stereotype.Component;

import com.kb.wms.auth.application.port.out.StoreOwnerAssignPort;
import com.kb.wms.store.application.port.in.StoreMemberUseCase;
import com.kb.wms.store.application.port.in.command.StoreMemberAssignCommand;

import lombok.RequiredArgsConstructor;

/**
 * 점주 배정 포트를 지점 도메인의 멤버 배정 유스케이스에 연결한다. 지점이 정한 역할 코드는 이 어댑터만 안다.
 */
@Component
@RequiredArgsConstructor
public class StoreOwnerAssignAdapter implements StoreOwnerAssignPort {

    private static final String OWNER_ROLE_CODE = "OWNER";

    private final StoreMemberUseCase storeMemberUseCase;

    @Override
    public void assignOwner(Long storeId, Long userId) {
        storeMemberUseCase.assignManager(new StoreMemberAssignCommand(storeId, userId, OWNER_ROLE_CODE));
    }
}
