package com.kb.wms.inbound.adapter.in.web.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.kb.wms.inbound.application.port.in.command.InboundInspectCommand;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * PATCH /api/v1/inbounds/{inboundId}/inspect 요청 바디.
 * lines는 이번 입고의 검수 항목 전체이며, 호출할 때마다 기존 검수 항목을 이 목록으로 통째로 교체한다.
 */
public record InboundInspectRequest(
        @NotEmpty(message = "검수 항목은 1개 이상이어야 합니다.")
        @Valid
        List<Line> lines
) {

    public record Line(
            @NotNull(message = "발주 항목 ID는 필수 값입니다.")
            Long purchaseOrderLineId,

            @NotBlank(message = "로트 번호는 필수 값입니다.")
            @Size(max = 100, message = "로트 번호는 최대 100자입니다.")
            String lotNumber,

            LocalDate manufacturedDate,

            LocalDate expiryDate,

            @NotNull(message = "입고 수량은 필수 값입니다.")
            @Positive(message = "입고 수량은 0보다 커야 합니다.")
            Long receivedQuantity,

            @NotNull(message = "합격 수량은 필수 값입니다.")
            @PositiveOrZero(message = "합격 수량은 0 이상이어야 합니다.")
            Long acceptedQuantity,

            @NotNull(message = "불량 수량은 필수 값입니다.")
            @PositiveOrZero(message = "불량 수량은 0 이상이어야 합니다.")
            Long defectiveQuantity,

            @NotNull(message = "입고 단가는 필수 값입니다.")
            @DecimalMin(value = "0", message = "입고 단가는 0 이상이어야 합니다.")
            @Digits(integer = 16, fraction = 2, message = "입고 단가는 소수 2자리까지 입력할 수 있습니다.")
            BigDecimal receivedUnitPrice,

            @Size(max = 500, message = "단가 변경 사유는 최대 500자입니다.")
            String priceChangeReason,

            @Size(max = 1000, message = "검수 비고는 최대 1000자입니다.")
            String inspectionNote,

            Long acceptedSectionId,

            Long defectSectionId
    ) {
    }

    /**
     * @param userId 검수 처리자. 토큰 사용자의 ID다.
     */
    public InboundInspectCommand toCommand(Long userId) {
        List<InboundInspectCommand.Line> commandLines = lines.stream()
                .map(line -> new InboundInspectCommand.Line(
                        line.purchaseOrderLineId(), line.lotNumber(), line.manufacturedDate(), line.expiryDate(),
                        line.receivedQuantity(), line.acceptedQuantity(), line.defectiveQuantity(),
                        line.receivedUnitPrice(), line.priceChangeReason(), line.inspectionNote(),
                        line.acceptedSectionId(), line.defectSectionId()))
                .toList();
        return new InboundInspectCommand(userId, commandLines);
    }
}
