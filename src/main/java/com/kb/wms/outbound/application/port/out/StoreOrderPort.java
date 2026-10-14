package com.kb.wms.outbound.application.port.out;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import com.kb.wms.outbound.domain.enums.StoreOrderState;

/**
 * 출고가 지점 발주를 읽고 항목 수량·발주 상태를 바꿀 때 쓰는 아웃바운드 포트.
 * 출고는 발주의 엔티티·enum을 모르고 이 포트의 타입만 쓴다. 변환은 구현 어댑터가 한다.
 *
 * <p>쓰기 메서드는 호출한 서비스의 트랜잭션에 참여하며, 상태 확인과 변경은 호출 쪽이 발주·항목을 잠근 상태에서 한다.
 */
public interface StoreOrderPort {

    /** 발주 헤더를 조회한다. 없으면 404 STORE_ORDER_NOT_FOUND. */
    OrderSnapshot getOrder(Long storeOrderId);

    /** 비관적 쓰기 락으로 발주 헤더를 조회한다. 없으면 404 STORE_ORDER_NOT_FOUND. */
    OrderSnapshot getOrderForUpdate(Long storeOrderId);

    /** 발주 항목을 항목 ID 오름차순으로 조회한다. */
    List<LineSnapshot> getLines(Long storeOrderId);

    /** 발주 항목을 비관적 쓰기 락으로 항목 ID 오름차순 조회한다. */
    List<LineSnapshot> getLinesForUpdate(Long storeOrderId);

    /** 재고 할당에 따라 항목의 할당 수량을 늘린다. */
    void increaseAllocated(List<LineQuantity> quantities);

    /** 할당 해제·발주 취소에 따라 항목의 할당 수량을 줄인다. */
    void decreaseAllocated(List<LineQuantity> quantities);

    /** 피킹 완료 반영: 항목의 할당 수량을 줄이고 출고 수량을 늘린다. */
    void applyPicked(List<LinePicked> picked);

    /** 배송 완료 후 항목 상태를 출고 수량 기준으로 다시 계산한다. */
    void refreshLineStatuses(Collection<Long> storeOrderLineIds);

    /**
     * 모든 항목이 전량 출고되었고 발주가 ASSIGNED면 COMPLETED로 바꾼다. 조건이 안 맞으면 바꾸지 않고 현재 발주를 돌려준다.
     *
     * @param changedBy 이력의 처리자(요청자)
     */
    OrderSnapshot completeIfFulfilled(Long storeOrderId, Long changedBy);

    /** 출고가 필요로 하는 발주 헤더 값. */
    record OrderSnapshot(Long storeOrderId, String orderNo, Long warehouseId, StoreOrderState state) {
    }

    /**
     * 출고가 필요로 하는 발주 항목 값.
     *
     * @param unallocatedQuantity      아직 재고를 할당하지 못한 수량
     * @param requestedUnitSupplyPrice 발주 시점 공급 단가 스냅샷(없을 수 있다)
     */
    record LineSnapshot(Long storeOrderLineId, Long skuId, long unallocatedQuantity,
                        BigDecimal requestedUnitSupplyPrice) {
    }

    /** 항목 하나의 수량 변경(할당 증가·감소). */
    record LineQuantity(Long storeOrderLineId, long quantity) {
    }

    /** 항목 하나의 피킹 반영: 줄일 할당 수량과 늘릴 출고 수량. */
    record LinePicked(Long storeOrderLineId, long allocatedQuantity, long pickedQuantity) {
    }
}
