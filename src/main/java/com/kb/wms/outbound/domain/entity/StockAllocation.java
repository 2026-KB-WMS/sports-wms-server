package com.kb.wms.outbound.domain.entity;

import com.kb.wms.outbound.domain.enums.AllocationStatus;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 재고 할당. 발주 항목 하나가 특정 재고 LOT 에서 예약한 수량.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockAllocation {

    private Long allocationId;
    private Long storeOrderLineId;
    private Long inventoryLotId;
    private long allocatedQuantity;
    private long pickedQuantity;
    private AllocationStatus status;
    private LocalDateTime allocatedAt;
    private LocalDateTime releasedAt;
    private Long allocatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    private StockAllocation(Long allocationId, Long storeOrderLineId, Long inventoryLotId,
                            long allocatedQuantity, long pickedQuantity, AllocationStatus status,
                            LocalDateTime allocatedAt, LocalDateTime releasedAt, Long allocatedBy,
                            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.allocationId = allocationId;
        this.storeOrderLineId = storeOrderLineId;
        this.inventoryLotId = inventoryLotId;
        this.allocatedQuantity = allocatedQuantity;
        this.pickedQuantity = pickedQuantity;
        this.status = status;
        this.allocatedAt = allocatedAt;
        this.releasedAt = releasedAt;
        this.allocatedBy = allocatedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static StockAllocation allocate(Long storeOrderLineId, Long inventoryLotId,
                                           long allocatedQuantity, Long allocatedBy,
                                           LocalDateTime allocatedAt) {
        if (storeOrderLineId == null || inventoryLotId == null) {
            throw new IllegalArgumentException("발주 항목과 재고 LOT 은 필수입니다.");
        }
        if (allocatedQuantity <= 0) {
            throw new IllegalArgumentException("할당 수량은 1 이상이어야 합니다.");
        }
        return StockAllocation.builder()
                .storeOrderLineId(storeOrderLineId)
                .inventoryLotId(inventoryLotId)
                .allocatedQuantity(allocatedQuantity)
                .pickedQuantity(0)
                .status(AllocationStatus.ALLOCATED)
                .allocatedAt(allocatedAt)
                .allocatedBy(allocatedBy)
                .build();
    }

    /** 할당 해제. ALLOCATED 상태에서만 가능. */
    public void release(LocalDateTime releasedAt) {
        requireStatus(AllocationStatus.ALLOCATED, "할당 해제는 ALLOCATED 상태에서만 가능합니다.");
        this.status = AllocationStatus.RELEASED;
        this.releasedAt = releasedAt;
    }

    /** 피킹 완료. 0 이상, 할당 수량 이하의 실제 피킹 수량을 기록한다. */
    public void pick(long pickedQuantity) {
        requireStatus(AllocationStatus.ALLOCATED, "피킹 완료는 ALLOCATED 상태에서만 가능합니다.");
        if (pickedQuantity < 0 || pickedQuantity > allocatedQuantity) {
            throw new IllegalArgumentException("피킹 수량은 0 이상, 할당 수량 이하여야 합니다.");
        }
        this.pickedQuantity = pickedQuantity;
        this.status = AllocationStatus.PICKED;
    }

    /** 피킹 부족 수량 (할당 - 피킹). */
    public long shortageQuantity() {
        return allocatedQuantity - pickedQuantity;
    }

    public boolean isActive() {
        return status == AllocationStatus.ALLOCATED;
    }

    private void requireStatus(AllocationStatus expected, String message) {
        if (this.status != expected) {
            throw new IllegalStateException(message);
        }
    }
}
