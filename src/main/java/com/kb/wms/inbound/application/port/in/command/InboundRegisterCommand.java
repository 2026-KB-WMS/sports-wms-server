package com.kb.wms.inbound.application.port.in.command;

import java.time.LocalDateTime;

/**
 * POST /api/v1/inbounds 요청. 입고 대상 창고는 발주서가 속한 요청의 창고를 따르므로 요청으로 받지 않는다.
 *
 * @param supplierOrderId 입고 대상 공급처 발주서 ID (ISSUED·PARTIALLY_RECEIVED 상태)
 * @param arrivedAt       실제 도착 일시 (선택, 현재 시각 이전). 생략하면 요청 시각
 * @param note            입고 비고 (선택, 최대 1000자)
 * @param userId          등록 처리 사용자 (토큰 사용자의 ID). 상태 이력의 처리자로 기록한다
 */
public record InboundRegisterCommand(
        Long supplierOrderId,
        LocalDateTime arrivedAt,
        String note,
        Long userId
) {
}
