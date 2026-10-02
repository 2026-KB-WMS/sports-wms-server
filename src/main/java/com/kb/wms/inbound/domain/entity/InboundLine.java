package com.kb.wms.inbound.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 입고 검수 항목. 발주 항목(PurchaseOrderLine) 하나에 대해 로트별로 실제 받은 수량·합격/불량 수량·확정 단가·
 * 적치 구역을 기록한다. 한 발주 항목을 여러 로트로 나눠 받을 수 있어 같은 발주 항목이 로트를 달리해 여러 줄 나올 수 있다.
 * 검수(inspect) 호출마다 입고의 검수 항목 전체를 교체하므로 생성 후 수정하지 않는다.
 * 구역은 검수 시 미리 지정할 수 있지만 완료 전에는 지정되어 있어야 한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InboundLine {

    private Long inboundLineId;
    private Long inboundId;
    private Long purchaseOrderLineId;
    private Long lotId;
    private Long acceptedSectionId;
    private Long defectSectionId;
    private Long receivedQuantity;
    private Long acceptedQuantity;
    private Long defectiveQuantity;
    private BigDecimal receivedUnitPrice;
    private BigDecimal lineAmount;
    private String priceChangeReason;
    private String inspectionNote;
    private LocalDateTime receivedAt;
    private Long receivedBy;
    private LocalDateTime createdAt;

    @Builder
    private InboundLine(Long inboundLineId, Long inboundId, Long purchaseOrderLineId, Long lotId,
                        Long acceptedSectionId, Long defectSectionId, Long receivedQuantity, Long acceptedQuantity,
                        Long defectiveQuantity, BigDecimal receivedUnitPrice, BigDecimal lineAmount,
                        String priceChangeReason, String inspectionNote, LocalDateTime receivedAt, Long receivedBy,
                        LocalDateTime createdAt) {
        this.inboundLineId = inboundLineId;
        this.inboundId = inboundId;
        this.purchaseOrderLineId = purchaseOrderLineId;
        this.lotId = lotId;
        this.acceptedSectionId = acceptedSectionId;
        this.defectSectionId = defectSectionId;
        this.receivedQuantity = receivedQuantity;
        this.acceptedQuantity = acceptedQuantity == null ? 0L : acceptedQuantity;
        this.defectiveQuantity = defectiveQuantity == null ? 0L : defectiveQuantity;
        this.receivedUnitPrice = receivedUnitPrice;
        this.lineAmount = lineAmount;
        this.priceChangeReason = priceChangeReason;
        this.inspectionNote = inspectionNote;
        this.receivedAt = receivedAt;
        this.receivedBy = receivedBy;
        this.createdAt = createdAt;
    }

    /**
     * 검수 항목 등록. 입고 수량은 0 초과, 합격·불량 수량은 0 이상이며 두 수량의 합은 입고 수량과 같아야 한다.
     * 항목 금액은 입고 수량 × 입고 확정 단가로 계산한다.
     * 발주 잔여 수량 초과·단가 차이 사유·구역 유효성처럼 다른 애그리거트가 필요한 검증은 서비스에서 한다.
     */
    public static InboundLine register(Long inboundId, Long purchaseOrderLineId, Long lotId,
                                       Long acceptedSectionId, Long defectSectionId,
                                       long receivedQuantity, long acceptedQuantity, long defectiveQuantity,
                                       BigDecimal receivedUnitPrice, String priceChangeReason, String inspectionNote,
                                       LocalDateTime receivedAt, Long receivedBy) {
        if (receivedQuantity <= 0) {
            throw new IllegalArgumentException("입고 수량은 1 이상이어야 합니다.");
        }
        if (acceptedQuantity < 0 || defectiveQuantity < 0) {
            throw new IllegalArgumentException("합격·불량 수량은 0 이상이어야 합니다.");
        }
        if (acceptedQuantity + defectiveQuantity != receivedQuantity) {
            throw new IllegalArgumentException("합격 수량과 불량 수량의 합이 입고 수량과 같아야 합니다.");
        }
        if (receivedUnitPrice == null || receivedUnitPrice.signum() < 0) {
            throw new IllegalArgumentException("입고 단가는 0 이상이어야 합니다.");
        }
        return InboundLine.builder()
                .inboundId(inboundId)
                .purchaseOrderLineId(purchaseOrderLineId)
                .lotId(lotId)
                .acceptedSectionId(acceptedSectionId)
                .defectSectionId(defectSectionId)
                .receivedQuantity(receivedQuantity)
                .acceptedQuantity(acceptedQuantity)
                .defectiveQuantity(defectiveQuantity)
                .receivedUnitPrice(receivedUnitPrice)
                .lineAmount(receivedUnitPrice.multiply(BigDecimal.valueOf(receivedQuantity)))
                .priceChangeReason(priceChangeReason)
                .inspectionNote(inspectionNote)
                .receivedAt(receivedAt)
                .receivedBy(receivedBy)
                .build();
    }

    /** 합격 수량이 있으면 합격 구역이, 불량 수량이 있으면 불량 구역이 지정되어 있는지. 완료(complete) 전 확인 기준이다. */
    public boolean hasRequiredSections() {
        boolean acceptedOk = this.acceptedQuantity == 0 || this.acceptedSectionId != null;
        boolean defectOk = this.defectiveQuantity == 0 || this.defectSectionId != null;
        return acceptedOk && defectOk;
    }
}
