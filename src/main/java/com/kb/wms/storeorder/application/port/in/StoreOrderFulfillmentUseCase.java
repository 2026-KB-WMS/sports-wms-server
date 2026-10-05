package com.kb.wms.storeorder.application.port.in;

import java.util.Collection;
import java.util.List;

import com.kb.wms.storeorder.application.port.in.command.StoreOrderLinePickedCommand;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderLineQuantityCommand;
import com.kb.wms.storeorder.domain.entity.StoreOrder;
import com.kb.wms.storeorder.domain.entity.StoreOrderLine;

/**
 * 출고 도메인이 지점 발주를 읽고 항목 수량·발주 상태를 바꿀 때 쓰는 인바운드 포트(출고 → 지점 발주 방향).
 *
 * <p>쓰기 메서드는 호출한 출고 서비스의 트랜잭션에 반드시 참여한다(진행 중인 트랜잭션이 없으면 예외).
 * 상태 확인과 변경은 호출 쪽이 발주·항목을 잠근 상태에서 한다.
 * 이 포트는 {@code StoreOrderService}와 분리돼 있다. 지점 발주 서비스가 출고 연동 포트를 거쳐 출고 어댑터를 부르므로,
 * 같은 서비스에 두면 빈 순환 의존이 생긴다.
 */
public interface StoreOrderFulfillmentUseCase {

    /** 발주 헤더를 조회한다. 없으면 404 STORE_ORDER_NOT_FOUND. */
    StoreOrder getOrder(Long storeOrderId);

    /** 비관적 쓰기 락으로 발주 헤더를 조회한다. 없으면 404 STORE_ORDER_NOT_FOUND. */
    StoreOrder getOrderForUpdate(Long storeOrderId);

    /** 발주 항목을 항목 ID 오름차순으로 조회한다. */
    List<StoreOrderLine> getLines(Long storeOrderId);

    /** 발주 항목을 비관적 쓰기 락으로 항목 ID 오름차순 조회한다. */
    List<StoreOrderLine> getLinesForUpdate(Long storeOrderId);

    /** 재고 할당에 따라 항목의 할당 수량을 늘린다. 잔여 수량을 넘으면 거절한다. */
    void increaseAllocated(List<StoreOrderLineQuantityCommand> commands);

    /** 할당 해제·발주 취소에 따라 항목의 할당 수량을 줄인다. */
    void decreaseAllocated(List<StoreOrderLineQuantityCommand> commands);

    /** 피킹 완료 반영: 항목의 할당 수량을 줄이고 출고 수량을 늘린다. */
    void applyPicked(List<StoreOrderLinePickedCommand> commands);

    /** 배송 완료 후 항목 상태(PARTIALLY_SHIPPED·COMPLETED)를 출고 수량 기준으로 다시 계산한다. */
    void refreshLineStatuses(Collection<Long> storeOrderLineIds);

    /**
     * 모든 항목이 전량 출고(shipped ≥ requested)되었고 발주가 ASSIGNED면 COMPLETED로 바꾸고 상태 이력을 남긴다(시스템 자동 전이,
     * 처리자는 요청자). 호출 쪽은 진행 중 출고가 없을 때만 부른다. 조건이 안 맞으면 바꾸지 않고 현재 발주를 돌려준다.
     */
    StoreOrder completeIfFulfilled(Long storeOrderId, Long changedBy);
}
