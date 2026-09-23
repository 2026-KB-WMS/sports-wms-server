package com.kb.wms.inventory.domain.entity;

import java.time.LocalDateTime;

import com.kb.wms.inventory.domain.enums.QualityStatus;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 특정 구역(section)의 특정 로트(lot) 실재고·할당 수량을 관리하는 재고 원장이다.
 *
 * <p>수량 규칙: 0 &lt;= allocated &lt;= onHand. 가용 수량은 onHand - allocated.
 * 업무 오류(가용 재고 부족 등)는 서비스 계층에서 {@link #canAllocate}, {@link #canDecrease} 등으로
 * 먼저 검증해 도메인 오류 코드로 응답하고, 이 클래스의 예외는 검증 누락을 막는 최후 방어선이다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryLot {

    private Long inventoryLotId;
    private Long sectionId;
    private Long lotId;
    private Long onHandQuantity;
    private Long allocatedQuantity;
    private QualityStatus qualityStatus;
    private LocalDateTime lastCountedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    private InventoryLot(Long inventoryLotId, Long sectionId, Long lotId, Long onHandQuantity,
                         Long allocatedQuantity, QualityStatus qualityStatus, LocalDateTime lastCountedAt,
                         LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.inventoryLotId = inventoryLotId;
        this.sectionId = sectionId;
        this.lotId = lotId;
        this.onHandQuantity = onHandQuantity == null ? 0L : onHandQuantity;
        this.allocatedQuantity = allocatedQuantity == null ? 0L : allocatedQuantity;
        this.qualityStatus = qualityStatus == null ? QualityStatus.AVAILABLE : qualityStatus;
        this.lastCountedAt = lastCountedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 수량 0인 새 재고 원장을 만든다. 수량은 {@link #increase}로 채운다.
     */
    public static InventoryLot open(Long sectionId, Long lotId, QualityStatus qualityStatus) {
        return InventoryLot.builder()
                .sectionId(sectionId)
                .lotId(lotId)
                .qualityStatus(qualityStatus)
                .build();
    }

    // ---------- 조회 ----------

    public long availableQuantity() {
        return this.onHandQuantity - this.allocatedQuantity;
    }

    public boolean isDefective() {
        return this.qualityStatus == QualityStatus.DEFECTIVE;
    }

    /** 가용(품질 AVAILABLE) 재고이면서 가용 수량이 충분한지 */
    public boolean canAllocate(long quantity) {
        return quantity > 0 && !isDefective() && availableQuantity() >= quantity;
    }

    /** 할당되지 않은 보유 수량에서 차감 가능한지 (조정 감소 등) */
    public boolean canDecrease(long quantity) {
        return quantity > 0 && availableQuantity() >= quantity;
    }

    public boolean canRelease(long quantity) {
        return quantity > 0 && this.allocatedQuantity >= quantity;
    }

    /** 할당분을 출고(차감)할 수 있는지 */
    public boolean canShip(long quantity) {
        return canRelease(quantity);
    }

    // ---------- 수량 변경 ----------

    /** 보유 수량 증가 (입고, 조정 증가) */
    public void increase(long quantity) {
        requirePositive(quantity);
        this.onHandQuantity += quantity;
    }

    /** 할당되지 않은 보유 수량 감소 (조정 감소) */
    public void decrease(long quantity) {
        requireState(canDecrease(quantity), "가용 수량보다 많이 차감할 수 없습니다.");
        this.onHandQuantity -= quantity;
    }

    /** 가용 수량을 출고용으로 예약 */
    public void allocate(long quantity) {
        requireState(canAllocate(quantity), "가용 수량보다 많이 할당할 수 없습니다.");
        this.allocatedQuantity += quantity;
    }

    /** 할당 해제 (출고 취소 등) */
    public void release(long quantity) {
        requireState(canRelease(quantity), "할당 수량보다 많이 해제할 수 없습니다.");
        this.allocatedQuantity -= quantity;
    }

    /** 할당분 출고: 할당 수량과 보유 수량을 함께 차감 */
    public void ship(long quantity) {
        requireState(canShip(quantity), "할당 수량보다 많이 출고할 수 없습니다.");
        this.allocatedQuantity -= quantity;
        this.onHandQuantity -= quantity;
    }

    public void markCounted(LocalDateTime countedAt) {
        this.lastCountedAt = countedAt;
    }

    private static void requirePositive(long quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 0보다 커야 합니다.");
        }
    }

    private static void requireState(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
