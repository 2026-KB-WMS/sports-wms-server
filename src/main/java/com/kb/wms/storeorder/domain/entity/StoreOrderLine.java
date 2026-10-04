package com.kb.wms.storeorder.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.kb.wms.storeorder.domain.enums.StoreOrderLineStatus;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 지점 발주의 SKU별 요청·할당·출고 수량 상세.
 * 상태는 REQUESTED → PARTIALLY_SHIPPED → COMPLETED로 갱신되고(출고 도메인이 shippedQuantity를 누적),
 * 발주가 취소·반려되면 REQUESTED 항목이 CANCELED가 된다.
 * 발주당 SKU는 한 줄만 둘 수 있다 (UNIQUE(store_order_id, sku_id)).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreOrderLine {

    private Long storeOrderLineId;
    private Long storeOrderId;
    private Long skuId;
    private Long requestedQuantity;
    private Long allocatedQuantity;
    private Long shippedQuantity;
    /** 등록 시점 ProductSKU.current_supply_price 스냅샷. */
    private BigDecimal requestedUnitSupplyPrice;
    private StoreOrderLineStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    private StoreOrderLine(Long storeOrderLineId, Long storeOrderId, Long skuId, Long requestedQuantity,
                           Long allocatedQuantity, Long shippedQuantity, BigDecimal requestedUnitSupplyPrice,
                           StoreOrderLineStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.storeOrderLineId = storeOrderLineId;
        this.storeOrderId = storeOrderId;
        this.skuId = skuId;
        this.requestedQuantity = requestedQuantity;
        this.allocatedQuantity = allocatedQuantity == null ? 0L : allocatedQuantity;
        this.shippedQuantity = shippedQuantity == null ? 0L : shippedQuantity;
        this.requestedUnitSupplyPrice = requestedUnitSupplyPrice;
        this.status = status == null ? StoreOrderLineStatus.REQUESTED : status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** 발주 항목 등록. 요청 수량은 1 이상, 공급 단가 스냅샷은 0 이상이어야 한다. */
    public static StoreOrderLine register(Long storeOrderId, Long skuId, long requestedQuantity,
                                          BigDecimal requestedUnitSupplyPrice) {
        if (requestedQuantity <= 0) {
            throw new IllegalArgumentException("요청 수량은 1 이상이어야 합니다.");
        }
        if (requestedUnitSupplyPrice == null || requestedUnitSupplyPrice.signum() < 0) {
            throw new IllegalArgumentException("공급 단가는 0 이상이어야 합니다.");
        }
        return StoreOrderLine.builder()
                .storeOrderId(storeOrderId)
                .skuId(skuId)
                .requestedQuantity(requestedQuantity)
                .requestedUnitSupplyPrice(requestedUnitSupplyPrice)
                .status(StoreOrderLineStatus.REQUESTED)
                .build();
    }

    /** 발주 취소·반려에 따른 항목 취소. 출고가 시작되기 전(REQUESTED)인 항목만 가능하다. */
    public void cancel() {
        if (this.status != StoreOrderLineStatus.REQUESTED) {
            throw new IllegalStateException("출고 전 상태의 발주 항목만 취소할 수 있습니다. 현재 상태: " + this.status);
        }
        this.status = StoreOrderLineStatus.CANCELED;
    }

    /** 항목 금액. 요청 수량 × 공급 단가 스냅샷이며 저장하지 않고 계산한다. */
    public BigDecimal lineAmount() {
        return requestedUnitSupplyPrice.multiply(BigDecimal.valueOf(requestedQuantity));
    }

    /** 전량 출고되었는지. 출고 수량이 요청 수량 이상이면 true. */
    public boolean isFulfilled() {
        return this.shippedQuantity >= this.requestedQuantity;
    }

    /** 아직 출고되지 않은 수량(부족분). 초과 출고여도 음수가 되지 않는다. */
    public long remainingQuantity() {
        return Math.max(0L, this.requestedQuantity - this.shippedQuantity);
    }

    /** 발주 총액. 항목 금액의 합계다. */
    public static BigDecimal totalAmount(List<StoreOrderLine> lines) {
        return lines.stream()
                .map(StoreOrderLine::lineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 같은 SKU가 두 줄 이상이면 예외. 서비스는 이 검사 후 400으로 변환한다. */
    public static void requireDistinctSkus(List<StoreOrderLine> lines) {
        Set<Long> seen = new HashSet<>();
        for (StoreOrderLine line : lines) {
            if (!seen.add(line.getSkuId())) {
                throw new IllegalArgumentException("같은 SKU를 두 번 요청할 수 없습니다. skuId: " + line.getSkuId());
            }
        }
    }
}
