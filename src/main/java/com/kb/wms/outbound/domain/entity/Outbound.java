package com.kb.wms.outbound.domain.entity;

import com.kb.wms.outbound.domain.enums.OutboundStatus;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 출고. 지점 발주 하나에 대한 출고 작업 단위.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Outbound {

    private Long outboundId;
    private String outboundNo;
    private Long storeOrderId;
    private OutboundStatus status;
    private LocalDateTime shippedAt;
    private Long shippedBy;
    private LocalDateTime deliveredAt;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    private Outbound(Long outboundId, String outboundNo, Long storeOrderId, OutboundStatus status,
                     LocalDateTime shippedAt, Long shippedBy, LocalDateTime deliveredAt, String note,
                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.outboundId = outboundId;
        this.outboundNo = outboundNo;
        this.storeOrderId = storeOrderId;
        this.status = status;
        this.shippedAt = shippedAt;
        this.shippedBy = shippedBy;
        this.deliveredAt = deliveredAt;
        this.note = note;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Outbound create(String outboundNo, Long storeOrderId, String note) {
        if (outboundNo == null || outboundNo.isBlank()) {
            throw new IllegalArgumentException("출고 번호는 필수입니다.");
        }
        if (storeOrderId == null) {
            throw new IllegalArgumentException("지점 발주는 필수입니다.");
        }
        return Outbound.builder()
                .outboundNo(outboundNo)
                .storeOrderId(storeOrderId)
                .status(OutboundStatus.READY)
                .note(note)
                .build();
    }

    public void startPicking() {
        requireStatus(OutboundStatus.READY, "피킹 시작은 READY 상태에서만 가능합니다.");
        this.status = OutboundStatus.PICKING;
    }

    public void completePicking() {
        requireStatus(OutboundStatus.PICKING, "피킹 완료는 PICKING 상태에서만 가능합니다.");
        this.status = OutboundStatus.PICKED;
    }

    public void ship(LocalDateTime shippedAt, Long shippedBy) {
        requireStatus(OutboundStatus.PICKED, "출고 처리는 PICKED 상태에서만 가능합니다.");
        this.status = OutboundStatus.SHIPPED;
        this.shippedAt = shippedAt;
        this.shippedBy = shippedBy;
    }

    public void deliver(LocalDateTime deliveredAt) {
        requireStatus(OutboundStatus.SHIPPED, "배송 완료는 SHIPPED 상태에서만 가능합니다.");
        this.status = OutboundStatus.DELIVERED;
        this.deliveredAt = deliveredAt;
    }

    public void cancel() {
        requireStatus(OutboundStatus.READY, "출고 취소는 READY 상태에서만 가능합니다.");
        this.status = OutboundStatus.CANCELED;
    }

    /** 피킹이 시작되었는가 (PICKING 이후 상태, 취소 제외). */
    public boolean isPickingStarted() {
        return status != OutboundStatus.READY && status != OutboundStatus.CANCELED;
    }

    /** 진행 중인가 (종료 상태가 아님). */
    public boolean isInProgress() {
        return !isTerminal();
    }

    public boolean isTerminal() {
        return status == OutboundStatus.DELIVERED || status == OutboundStatus.CANCELED;
    }

    private void requireStatus(OutboundStatus expected, String message) {
        if (this.status != expected) {
            throw new IllegalStateException(message);
        }
    }
}
