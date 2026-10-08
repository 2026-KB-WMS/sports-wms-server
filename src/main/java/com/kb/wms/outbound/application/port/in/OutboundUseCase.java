package com.kb.wms.outbound.application.port.in;

import java.util.List;

import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.outbound.application.port.in.command.OutboundCancelCommand;
import com.kb.wms.outbound.application.port.in.command.OutboundCreateCommand;
import com.kb.wms.outbound.application.port.in.query.OutboundSearchCondition;
import com.kb.wms.outbound.application.port.in.result.OutboundCancelResult;
import com.kb.wms.outbound.application.port.in.result.OutboundCreateResult;
import com.kb.wms.outbound.application.port.in.result.OutboundDetail;
import com.kb.wms.outbound.application.port.in.result.OutboundPickingStartResult;
import com.kb.wms.outbound.application.port.in.result.OutboundSummary;

/**
 * 출고 유스케이스(서비스 B): 출고 생성, 피킹 시작, 취소, 조회.
 * 피킹 완료·출고·배송 완료는 서비스 C(#150)가 맡는다.
 */
public interface OutboundUseCase {

    /**
     * 배정된(ASSIGNED) 발주에서 출고에 아직 묶이지 않은 ALLOCATED 할당 전부를 한 출고(READY)로 묶는다.
     * 재고·할당·발주 수량은 바뀌지 않는다.
     */
    OutboundCreateResult createOutbound(OutboundCreateCommand command, AuthenticatedUser actor);

    /** READY 출고를 PICKING으로 바꾼다. 이후 연결된 할당은 해제할 수 없고 발주도 취소할 수 없다. */
    OutboundPickingStartResult startPicking(Long outboundId, AuthenticatedUser actor);

    /** READY 출고만 취소한다. 재고·할당·발주 상태는 그대로 두고, 할당은 다시 묶거나 해제할 수 있게 된다. */
    OutboundCancelResult cancel(OutboundCancelCommand command, AuthenticatedUser actor);

    List<OutboundSummary> searchOutbounds(OutboundSearchCondition condition, AuthenticatedUser actor);

    OutboundDetail getOutbound(Long outboundId, AuthenticatedUser actor);
}
