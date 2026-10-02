package com.kb.wms.inbound.domain.entity;

import java.time.LocalDateTime;

import com.kb.wms.inbound.domain.enums.InboundStatus;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 발주에 대한 실제 입고(도착) 처리를 기록하는 입고 헤더.
 * 상태 전이: ARRIVED → INSPECTING → COMPLETED, ARRIVED·INSPECTING → CANCELED.
 * 하나의 발주에 부분 입고가 여러 번 걸칠 수 있어 같은 발주에 입고를 여러 건 둘 수 있다.
 * 검수 항목(InboundLine)은 inboundId로 참조하며 이 객체가 직접 들고 있지 않는다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inbound {

    private Long inboundId;
    private String inboundNo;
    private Long purchaseOrderId;
    private Long warehouseId;
    private InboundStatus status;
    private LocalDateTime arrivedAt;
    private LocalDateTime receivedAt;
    private Long receivedBy;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    private Inbound(Long inboundId, String inboundNo, Long purchaseOrderId, Long warehouseId, InboundStatus status,
                    LocalDateTime arrivedAt, LocalDateTime receivedAt, Long receivedBy, String note,
                    LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.inboundId = inboundId;
        this.inboundNo = inboundNo;
        this.purchaseOrderId = purchaseOrderId;
        this.warehouseId = warehouseId;
        this.status = status == null ? InboundStatus.ARRIVED : status;
        this.arrivedAt = arrivedAt;
        this.receivedAt = receivedAt;
        this.receivedBy = receivedBy;
        this.note = note;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** 도착 등록. 입고 대상 창고는 발주의 창고를 따르며, 도착 일시가 없으면 호출 측이 현재 시각을 채워 넘긴다. */
    public static Inbound register(String inboundNo, Long purchaseOrderId, Long warehouseId,
                                   LocalDateTime arrivedAt, String note) {
        return Inbound.builder()
                .inboundNo(inboundNo)
                .purchaseOrderId(purchaseOrderId)
                .warehouseId(warehouseId)
                .arrivedAt(arrivedAt)
                .note(note)
                .status(InboundStatus.ARRIVED)
                .build();
    }

    /**
     * 검수 진행. 처음 호출하면 ARRIVED → INSPECTING으로 전환하고, 이미 INSPECTING이면 상태를 유지한다
     * (검수 항목을 다시 교체하는 재호출). COMPLETED·CANCELED에서는 호출할 수 없다.
     */
    public void inspect() {
        if (this.status == InboundStatus.ARRIVED) {
            this.status = InboundStatus.INSPECTING;
            return;
        }
        if (this.status != InboundStatus.INSPECTING) {
            throw new IllegalStateException("도착 또는 검수 중 상태의 입고만 검수할 수 있습니다. 현재 상태: " + this.status);
        }
    }

    /** 입고 완료. INSPECTING에서만 가능하며 검수 완료 일시와 처리 사용자를 기록한다. */
    public void complete(Long receivedBy, LocalDateTime receivedAt) {
        if (this.status != InboundStatus.INSPECTING) {
            throw new IllegalStateException("검수 중 상태의 입고만 완료 처리할 수 있습니다. 현재 상태: " + this.status);
        }
        this.status = InboundStatus.COMPLETED;
        this.receivedBy = receivedBy;
        this.receivedAt = receivedAt;
    }

    /** 취소. ARRIVED·INSPECTING에서만 가능하다(재고 반영 전). 완료된 입고는 취소할 수 없다. */
    public void cancel() {
        if (!isCancelable()) {
            throw new IllegalStateException("도착 또는 검수 중 상태의 입고만 취소할 수 있습니다. 현재 상태: " + this.status);
        }
        this.status = InboundStatus.CANCELED;
    }

    public boolean isArrived() {
        return this.status == InboundStatus.ARRIVED;
    }

    public boolean isInspecting() {
        return this.status == InboundStatus.INSPECTING;
    }

    /** 검수 항목을 저장·교체할 수 있는 상태(ARRIVED·INSPECTING). */
    public boolean isInspectable() {
        return isArrived() || isInspecting();
    }

    public boolean isCancelable() {
        return isInspectable();
    }
}
