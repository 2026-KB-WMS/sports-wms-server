package com.kb.wms.store.application.port.in;

import java.util.List;

import com.kb.wms.common.security.AuthenticatedUser;
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

    /** 사용자 요청용 단건 조회. 담당 지점(본사는 전체)이 아니면 403 FORBIDDEN. 다른 도메인의 내부 조회는 주체 없는 버전을 쓴다. */
    Store getStore(Long storeId, AuthenticatedUser actor);

    Store updateStore(Long storeId, StoreUpdateCommand command);

    /**
     * 지점을 비활성화하고 상태 이력(StatusHistory)에 기록한다. reason은 선택이며 최대 500자.
     * userId는 처리자(changed_by)이다.
     */
    Store deactivateStore(Long storeId, String reason, Long userId);

    /** 비활성 지점을 다시 활성화하고 상태 이력에 기록한다. 이미 활성이면 409 CONFLICT. userId는 처리자(changed_by)이다. */
    Store activateStore(Long storeId, Long userId);

    /**
     * 로그인한 점주가 배정된 지점을 모두 조회한다.
     * 한 사용자가 여러 지점에 배정될 수 있어 목록으로 반환하며, 지점 정보와 배정(StoreMember) 정보를 함께 담는다.
     * 배정된 지점이 없으면 오류가 아니라 빈 목록을 반환한다.
     */
    List<StoreMembershipSummary> getMyStores(Long userId);
}
