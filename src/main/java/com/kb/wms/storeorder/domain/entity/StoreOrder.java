package com.kb.wms.storeorder.domain.entity;

import java.time.LocalDateTime;

import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 지점이 창고에 상품 보충을 요청하는 지점 발주 헤더.
 * 상태 전이: REQUESTED → APPROVED → ASSIGNED ↔ ON_HOLD, ASSIGNED → COMPLETED,
 * REQUESTED → REJECTED, REQUESTED·APPROVED·ASSIGNED·ON_HOLD → CANCELED.
 * 발주 항목(StoreOrderLine)은 storeOrderId로 참조하며 이 객체가 직접 들고 있지 않는다.
 * 권한·사유 필수 여부·출고 연동 검사는 서비스에서 하고, 이 객체는 상태 전이 가드만 맡는다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreOrder {

    private Long storeOrderId;
    private String orderNo;
    private Long storeId;
    /** 창고 배정(ASSIGNED) 전까지 null. */
    private Long warehouseId;
    private StoreOrderStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime requestedDeliveryAt;
    private String note;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    private StoreOrder(Long storeOrderId, String orderNo, Long storeId, Long warehouseId, StoreOrderStatus status,
                       LocalDateTime requestedAt, LocalDateTime requestedDeliveryAt, String note, Long createdBy,
                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.storeOrderId = storeOrderId;
        this.orderNo = orderNo;
        this.storeId = storeId;
        this.warehouseId = warehouseId;
        this.status = status == null ? StoreOrderStatus.REQUESTED : status;
        this.requestedAt = requestedAt;
        this.requestedDeliveryAt = requestedDeliveryAt;
        this.note = note;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** 지점 발주 등록. 창고는 아직 정해지지 않았으므로 warehouseId는 null이다. */
    public static StoreOrder register(String orderNo, Long storeId, LocalDateTime requestedAt,
                                      LocalDateTime requestedDeliveryAt, String note, Long createdBy) {
        return StoreOrder.builder()
                .orderNo(orderNo)
                .storeId(storeId)
                .requestedAt(requestedAt)
                .requestedDeliveryAt(requestedDeliveryAt)
                .note(note)
                .createdBy(createdBy)
                .status(StoreOrderStatus.REQUESTED)
                .build();
    }

    /** 본사 관리자 승인. REQUESTED에서만 가능하다. */
    public void approve() {
        requireStatus(StoreOrderStatus.REQUESTED, "요청 상태의 발주만 승인할 수 있습니다.");
        this.status = StoreOrderStatus.APPROVED;
    }

    /** 본사 관리자 반려. REQUESTED에서만 가능하다. 사유 필수 검증은 서비스에서 한다. */
    public void reject() {
        requireStatus(StoreOrderStatus.REQUESTED, "요청 상태의 발주만 반려할 수 있습니다.");
        this.status = StoreOrderStatus.REJECTED;
    }

    /**
     * 취소. REQUESTED(작성자 점주)·APPROVED·ASSIGNED·ON_HOLD(본사 관리자)에서 가능하다.
     * 권한 구분, 피킹 시작 여부 검사, 할당 해제는 서비스에서 한다.
     */
    public void cancel() {
        if (!isInProgress()) {
            throw new IllegalStateException("진행 중인 발주만 취소할 수 있습니다. 현재 상태: " + this.status);
        }
        this.status = StoreOrderStatus.CANCELED;
    }

    /** 창고 배정. APPROVED에서만 가능하며 이때 warehouseId가 채워진다. */
    public void assign(Long warehouseId) {
        requireWarehouse(warehouseId);
        requireStatus(StoreOrderStatus.APPROVED, "승인 상태의 발주만 창고를 배정할 수 있습니다.");
        this.warehouseId = warehouseId;
        this.status = StoreOrderStatus.ASSIGNED;
    }

    /**
     * 창고 재배정. ASSIGNED에서만 가능하며(ON_HOLD는 불가) 현재와 다른 창고여야 한다.
     * 재고 할당·출고가 없는지는 서비스에서 확인한다.
     */
    public void reassign(Long warehouseId) {
        requireWarehouse(warehouseId);
        requireStatus(StoreOrderStatus.ASSIGNED, "창고 배정 상태의 발주만 재배정할 수 있습니다.");
        if (warehouseId.equals(this.warehouseId)) {
            throw new IllegalArgumentException("현재 배정된 창고와 다른 창고를 지정해야 합니다.");
        }
        this.warehouseId = warehouseId;
    }

    /** 출고 보류. ASSIGNED에서만 가능하다. 피킹 시작 전 검사와 사유 필수 검증은 서비스에서 한다. */
    public void hold() {
        requireStatus(StoreOrderStatus.ASSIGNED, "창고 배정 상태의 발주만 출고를 보류할 수 있습니다.");
        this.status = StoreOrderStatus.ON_HOLD;
    }

    /** 출고 재개. ON_HOLD에서만 가능하다. */
    public void resume() {
        requireStatus(StoreOrderStatus.ON_HOLD, "출고 보류 상태의 발주만 재개할 수 있습니다.");
        this.status = StoreOrderStatus.ASSIGNED;
    }

    /**
     * 출고 종결. 전량 출고 완료(시스템)와 부분 출고 종결(창고 관리자) 모두 ASSIGNED에서 COMPLETED로 간다.
     * 전량인지 부분인지, 진행 중 출고가 없는지는 서비스에서 판단한다.
     */
    public void complete() {
        requireStatus(StoreOrderStatus.ASSIGNED, "창고 배정 상태의 발주만 출고 종결할 수 있습니다.");
        this.status = StoreOrderStatus.COMPLETED;
    }

    public boolean isRequested() {
        return this.status == StoreOrderStatus.REQUESTED;
    }

    /** 진행 중인 발주(REQUESTED·APPROVED·ASSIGNED·ON_HOLD). 취소 가능 여부와 지점 비활성화 가능 여부 판단 기준이다. */
    public boolean isInProgress() {
        return this.status == StoreOrderStatus.REQUESTED
                || this.status == StoreOrderStatus.APPROVED
                || this.status == StoreOrderStatus.ASSIGNED
                || this.status == StoreOrderStatus.ON_HOLD;
    }

    /** 종결 상태(CANCELED·REJECTED·COMPLETED). 같은 전이를 다시 요청하면 409로 다룬다. */
    public boolean isTerminal() {
        return !isInProgress();
    }

    private void requireStatus(StoreOrderStatus expected, String message) {
        if (this.status != expected) {
            throw new IllegalStateException(message + " 현재 상태: " + this.status);
        }
    }

    private static void requireWarehouse(Long warehouseId) {
        if (warehouseId == null) {
            throw new IllegalArgumentException("배정할 창고는 필수입니다.");
        }
    }
}
