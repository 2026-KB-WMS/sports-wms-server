package com.kb.wms.inventory.application.port.in;

import java.util.List;

import com.kb.wms.inventory.application.port.in.command.LotRegisterCommand;
import com.kb.wms.inventory.application.port.in.query.LotSearchCondition;
import com.kb.wms.inventory.application.port.in.result.LotSummary;
import com.kb.wms.inventory.domain.entity.Lot;

/**
 * 로트 조회·등록 유스케이스. GET /api/v1/lots, /lots/{lotId}
 * 로트는 별도 생성 API 없이 입고 검수 트랜잭션 안에서 {@link #findOrRegister}로 만든다(ADR-004).
 */
public interface LotUseCase {

    List<LotSummary> getLots(LotSearchCondition condition);

    LotSummary getLot(Long lotId);

    /**
     * SKU + 공급처 + 로트 번호로 로트를 찾고, 없으면 AVAILABLE 상태로 새로 만든다.
     * 기존 로트의 원가·일자가 요청과 다르거나 상태가 AVAILABLE이 아니면 거절한다.
     */
    Lot findOrRegister(LotRegisterCommand command);
}
