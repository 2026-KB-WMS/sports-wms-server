package com.kb.wms.storeorder.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.kb.wms.storeorder.application.port.in.result.StoreOrderFulfillmentCancelResult;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderOutboundView;
import com.kb.wms.storeorder.domain.enums.StoreOrderOutboundStatus;

/**
 * 출고·재고 할당 도메인 연동 아웃바운드 포트. 지점 발주 쪽이 정의하고, 구현은 출고 도메인의 어댑터가 맡는다
 * (outbound.adapter.out.storeorder.StoreOrderOutboundAdapter). 발주와 출고는 서로의 엔티티를 참조하지 않고 ID로만 연결한다.
 */
public interface StoreOrderOutboundPort {

    /** 발주에 딸린 출고 중 가장 최근(생성 순 마지막) 출고의 상태. 출고가 없으면 비어 있다. */
    Optional<StoreOrderOutboundStatus> findLatestOutboundStatus(Long storeOrderId);

    /** {@link #findLatestOutboundStatus}를 여러 발주에 대해 한 번에 조회한다(목록용). 출고가 없는 발주는 키가 없다. */
    Map<Long, StoreOrderOutboundStatus> findLatestOutboundStatuses(Collection<Long> storeOrderIds);

    /** 발주에 딸린 출고를 생성 순으로 나열한다. 취소된 출고도 포함한다. */
    List<StoreOrderOutboundView> findOutbounds(Long storeOrderId);

    /** 피킹이 시작된 출고(PICKING·PICKED·SHIPPED·DELIVERED)가 있는지. 취소·보류 가드(ORDER_IN_PICKING)에 쓴다. */
    boolean existsPickingStarted(Long storeOrderId);

    /** 진행 중 출고(READY·PICKING·PICKED·SHIPPED)가 있는지. 부분 출고 종결 가드(OUTBOUND_IN_PROGRESS)에 쓴다. */
    boolean existsInProgressOutbound(Long storeOrderId);

    /** 해제되지 않은 재고 할당(ALLOCATED)이나 취소되지 않은 출고가 있는지. 재배정 가드(ORDER_IN_FULFILLMENT)에 쓴다. */
    boolean existsActiveFulfillment(Long storeOrderId);

    /**
     * 승인 이후 발주 취소에 따라 ALLOCATED 재고 할당을 RELEASED로, READY 출고를 CANCELED로 바꾸고
     * 재고 행과 발주 항목의 allocated_quantity를 줄인다. 호출한 서비스의 트랜잭션에 참여하며, 정리한 건수를 돌려준다.
     * 이력 사유에는 "발주 취소로 인한 자동 처리"를 남긴다.
     *
     * @param changedBy 취소를 요청한 사용자(이력의 처리자)
     */
    StoreOrderFulfillmentCancelResult cancelFulfillment(Long storeOrderId, Long changedBy);

    /**
     * 부분 출고 종결 때 출고에 묶이지 않고 남은 ALLOCATED 재고 할당을 RELEASED로 바꾸고 재고 행과 발주 항목의
     * allocated_quantity를 줄인다. 출고는 취소하지 않으며, 호출한 서비스의 트랜잭션에 참여한다.
     * 이력 사유에는 "발주 부분 종결로 인한 자동 해제"를 남긴다.
     *
     * @param changedBy 종결을 요청한 사용자(이력의 처리자)
     * @return 해제한 할당 건수
     */
    int releaseUnlinkedAllocations(Long storeOrderId, Long changedBy);
}
