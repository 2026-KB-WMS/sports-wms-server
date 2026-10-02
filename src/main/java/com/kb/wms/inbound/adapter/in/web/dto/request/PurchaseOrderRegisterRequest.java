package com.kb.wms.inbound.adapter.in.web.dto.request;

import java.time.LocalDateTime;
import java.util.List;

import com.kb.wms.inbound.application.port.in.command.PurchaseOrderRegisterCommand;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * POST /api/v1/purchase-orders 요청 바디.
 * 발주 단가는 받지 않고 등록 시점의 SKU 매입 단가를 서비스가 스냅샷한다.
 */
public record PurchaseOrderRegisterRequest(
        @NotNull(message = "창고 ID는 필수 값입니다.")
        Long warehouseId,

        @NotNull(message = "공급처 ID는 필수 값입니다.")
        Long supplierId,

        LocalDateTime expectedAt,

        @Size(max = 1000, message = "비고는 최대 1000자입니다.")
        String note,

        @NotEmpty(message = "발주 항목은 1개 이상이어야 합니다.")
        @Valid
        List<Line> lines
) {

    public record Line(
            @NotNull(message = "SKU ID는 필수 값입니다.")
            Long skuId,

            @NotNull(message = "발주 수량은 필수 값입니다.")
            @Positive(message = "발주 수량은 0보다 커야 합니다.")
            Long expectedQuantity
    ) {
    }

    /**
     * @param userId 요청 사용자. 인증이 구현되지 않아 쿼리 파라미터로 받는다(인증 연동 시 토큰의 사용자로 대체).
     */
    public PurchaseOrderRegisterCommand toCommand(Long userId) {
        List<PurchaseOrderRegisterCommand.Line> commandLines = lines.stream()
                .map(line -> new PurchaseOrderRegisterCommand.Line(line.skuId(), line.expectedQuantity()))
                .toList();
        return new PurchaseOrderRegisterCommand(warehouseId, supplierId, expectedAt, note, userId, commandLines);
    }
}
