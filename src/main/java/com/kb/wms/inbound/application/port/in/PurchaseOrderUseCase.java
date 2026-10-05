package com.kb.wms.inbound.application.port.in;

import java.util.List;

import com.kb.wms.inbound.application.port.in.command.PurchaseOrderCancelCommand;
import com.kb.wms.inbound.application.port.in.command.PurchaseOrderRegisterCommand;
import com.kb.wms.inbound.application.port.in.query.PurchaseOrderSearchCondition;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderDetails;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderSummary;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderView;
import com.kb.wms.inbound.domain.entity.PurchaseOrder;

/**
 * 발주 등록/조회/확정/취소 유스케이스.
 * POST, GET /api/v1/purchase-orders, GET .../{id}, GET .../{id}/details, PATCH .../{id}/confirm, PATCH .../{id}/cancel
 */
public interface PurchaseOrderUseCase {

    /** 발주와 발주 항목을 한 트랜잭션으로 등록하고 발주 ID를 반환한다. 응답 조립은 조회 유스케이스로 한다. */
    Long registerPurchaseOrder(PurchaseOrderRegisterCommand command);

    List<PurchaseOrderSummary> getPurchaseOrders(PurchaseOrderSearchCondition condition);

    PurchaseOrderView getPurchaseOrder(Long purchaseOrderId);

    PurchaseOrderDetails getPurchaseOrderDetails(Long purchaseOrderId);

    /** 확정 대기(REQUESTED) 발주를 확정한다. userId는 상태 이력의 처리자다. */
    PurchaseOrder confirmPurchaseOrder(Long purchaseOrderId, Long userId);

    /** 요청·확정 발주를 취소한다. 사유와 처리자는 상태 이력에 남기고 조회 시 cancelReason으로 돌려준다. */
    PurchaseOrder cancelPurchaseOrder(Long purchaseOrderId, PurchaseOrderCancelCommand command);
}
