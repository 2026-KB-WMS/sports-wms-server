package com.kb.wms.store.application.port.in;

import java.util.List;

import com.kb.wms.store.application.port.in.command.StoreRegisterCommand;
import com.kb.wms.store.application.port.in.command.StoreUpdateCommand;
import com.kb.wms.store.application.port.in.query.StoreSearchCondition;
import com.kb.wms.store.application.port.in.result.StoreMembershipSummary;
import com.kb.wms.store.domain.entity.Store;

/**
 * 지점 등록/조회/수정/비활성화 유스케이스.
 * POST, GET, PATCH /api/v1/stores, GET /api/v1/stores/my
 */
public interface StoreUseCase {

    Store registerStore(StoreRegisterCommand command);

    List<Store> getStores(StoreSearchCondition condition);

    Store getStore(Long storeId);

    Store updateStore(Long storeId, StoreUpdateCommand command);

    Store deactivateStore(Long storeId);

    /**
     * 로그인한 점주가 배정된 지점을 모두 조회한다.
     * 한 사용자가 여러 지점에 배정될 수 있어 목록으로 반환하며, 지점 정보와 배정(StoreMember) 정보를 함께 담는다.
     * 배정된 지점이 없으면 오류가 아니라 빈 목록을 반환한다.
     */
    List<StoreMembershipSummary> getMyStores(Long userId);
}
