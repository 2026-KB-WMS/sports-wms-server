package com.kb.wms.inventory.application.port.in;

import java.util.List;

import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.inventory.application.port.in.command.LotRegisterCommand;
import com.kb.wms.inventory.application.port.in.query.LotSearchCondition;
import com.kb.wms.inventory.application.port.in.result.LotInboundView;
import com.kb.wms.inventory.application.port.in.result.LotSummary;
import com.kb.wms.inventory.domain.entity.Lot;

/**
 * 로트 조회·등록 유스케이스. GET /api/v1/lots, /lots/{lotId}
 * 로트는 별도 생성 API 없이 입고 검수 트랜잭션 안에서 {@link #findOrRegister}로 만든다(ADR-004).
 */
public interface LotUseCase {

    /**
     * 로트 목록. skuId·supplierId 필터 대상이 없으면 404(SKU_NOT_FOUND·SUPPLIER_NOT_FOUND).
     * 창고 관리자에게는 담당 창고에 재고 또는 입고 완료 이력이 있는 로트만 보인다.
     */
    List<LotSummary> getLots(LotSearchCondition condition, AuthenticatedUser actor);

    /** 로트 한 건. 없으면 404 LOT_NOT_FOUND, 창고 관리자에게 보이지 않는 로트면 403 FORBIDDEN. */
    LotSummary getLot(Long lotId, AuthenticatedUser actor);

    /** 로트가 입고된 이력(입고 완료 건). 로트가 없으면 404 LOT_NOT_FOUND. 창고 관리자에게는 담당 창고의 입고만 보인다. */
    List<LotInboundView> getLotInbounds(Long lotId, AuthenticatedUser actor);

    /**
     * SKU + 공급처 + 로트 번호로 로트를 찾고, 없으면 AVAILABLE 상태로 새로 만든다.
     * 기존 로트의 원가·일자가 요청과 다르거나 상태가 AVAILABLE이 아니면 거절한다.
     */
    Lot findOrRegister(LotRegisterCommand command);
}
