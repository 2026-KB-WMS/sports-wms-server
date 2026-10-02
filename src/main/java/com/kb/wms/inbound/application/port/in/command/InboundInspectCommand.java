package com.kb.wms.inbound.application.port.in.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * PATCH /api/v1/inbounds/{inboundId}/inspect 요청. lines는 이번 입고의 검수 항목 전체이며,
 * 호출할 때마다 기존에 저장된 검수 항목을 이 목록으로 통째로 교체한다.
 *
 * @param userId 검수 처리 사용자 (토큰 사용자). 항목의 처리 사용자·시각으로 기록한다
 */
public record InboundInspectCommand(
        Long userId,
        List<Line> lines
) {

    /**
     * @param purchaseOrderLineId 이 검수가 대응하는 발주 항목 ID (이 입고의 발주에 속한 항목)
     * @param lotNumber           로트 번호 (최대 100자). 같은 SKU·공급처·로트 번호가 있으면 재사용
     * @param manufacturedDate    제조일자 (선택)
     * @param expiryDate          유통기한 (선택, 제조일 이후)
     * @param receivedQuantity    실제 입고(검수) 수량 (0 초과)
     * @param acceptedQuantity    검수 합격 수량 (0 이상)
     * @param defectiveQuantity   검수 불량 수량 (0 이상). 합격 + 불량 = 입고 수량
     * @param receivedUnitPrice   실제 입고 확정 단가 (0 이상, 소수 2자리까지)
     * @param priceChangeReason   발주 단가와 입고 단가가 다를 때 필수 (최대 500자)
     * @param inspectionNote      검수 비고 (선택, 최대 1000자)
     * @param acceptedSectionId   합격품을 둘 구역 ID (선택, 완료 전에는 지정 필요)
     * @param defectSectionId     불량품을 둘 구역 ID (선택, 완료 전에는 지정 필요)
     */
    public record Line(
            Long purchaseOrderLineId,
            String lotNumber,
            LocalDate manufacturedDate,
            LocalDate expiryDate,
            long receivedQuantity,
            long acceptedQuantity,
            long defectiveQuantity,
            BigDecimal receivedUnitPrice,
            String priceChangeReason,
            String inspectionNote,
            Long acceptedSectionId,
            Long defectSectionId
    ) {
    }
}
