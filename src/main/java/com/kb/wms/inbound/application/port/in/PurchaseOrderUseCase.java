package com.kb.wms.inbound.application.port.in;

import java.util.List;

import com.kb.wms.common.paging.PageCondition;
import com.kb.wms.common.paging.PageResult;
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.inbound.application.port.in.command.PurchaseOrderCancelCommand;
import com.kb.wms.inbound.application.port.in.command.PurchaseOrderConfirmCommand;
import com.kb.wms.inbound.application.port.in.command.PurchaseOrderRejectCommand;
import com.kb.wms.inbound.application.port.in.command.PurchaseOrderRegisterCommand;
import com.kb.wms.inbound.application.port.in.query.PurchaseOrderSearchCondition;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderConfirmResult;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderDetails;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderSummary;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderView;
import com.kb.wms.inbound.domain.entity.PurchaseOrder;

/**
 * 발주(요청) 등록/조회/확정/반려/취소 유스케이스.
 * POST, GET /api/v1/purchase-orders, GET .../{id}, GET .../{id}/details,
 * PATCH .../{id}/confirm, PATCH .../{id}/reject, PATCH .../{id}/cancel
 */
public interface PurchaseOrderUseCase {

    /** 발주와 발주 항목을 한 트랜잭션으로 등록하고 발주 ID를 반환한다. 응답 조립은 조회 유스케이스로 한다. */
    Long registerPurchaseOrder(PurchaseOrderRegisterCommand command, AuthenticatedUser actor);

    PageResult<PurchaseOrderSummary> getPurchaseOrders(PurchaseOrderSearchCondition condition, PageCondition page,
                                                       AuthenticatedUser actor);

    PurchaseOrderView getPurchaseOrder(Long purchaseOrderId, AuthenticatedUser actor);

    PurchaseOrderDetails getPurchaseOrderDetails(Long purchaseOrderId, AuthenticatedUser actor);

    /**
     * 확정 대기(REQUESTED) 발주를 확정한다. 라인별 공급처(SKU에 매핑된 활성 공급처)를 정하고, 공급처별로
     * 공급처 발주서를 한 건씩 만들어 라인을 연결한 뒤 발주를 CONFIRMED로 바꾼다. 하나라도 실패하면 아무것도 반영하지 않는다.
     */
    PurchaseOrderConfirmResult confirmPurchaseOrder(Long purchaseOrderId, PurchaseOrderConfirmCommand command);

    /** 확정 대기(REQUESTED) 발주를 사유와 함께 반려한다. 본사 관리자만 가능하다(그 외 403 FORBIDDEN). */
    PurchaseOrder rejectPurchaseOrder(Long purchaseOrderId, PurchaseOrderRejectCommand command, AuthenticatedUser actor);

    /**
     * 요청·확정 발주를 취소한다. 사유와 처리자는 상태 이력에 남기고 조회 시 cancelReason으로 돌려준다.
     * 요청(REQUESTED) 발주는 작성자인 창고 관리자만, 확정(CONFIRMED) 발주는 본사 관리자만 취소할 수 있다(그 외 403 FORBIDDEN).
     * 확정 발주를 취소하면 공급처 발주서도 함께 CANCELED로 바뀐다.
     */
    PurchaseOrder cancelPurchaseOrder(Long purchaseOrderId, PurchaseOrderCancelCommand command, AuthenticatedUser actor);
}
