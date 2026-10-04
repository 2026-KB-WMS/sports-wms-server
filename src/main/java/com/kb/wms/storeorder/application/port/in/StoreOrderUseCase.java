package com.kb.wms.storeorder.application.port.in;

import java.util.List;

import com.kb.wms.storeorder.application.port.in.command.StoreOrderAssignCommand;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderRegisterCommand;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderCancelCommand;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderCompletePartialCommand;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderHoldCommand;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderRejectCommand;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderResumeCommand;
import com.kb.wms.storeorder.application.port.in.query.StoreOrderSearchCondition;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderAssignResult;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderCancelResult;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderCompletePartialResult;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderDetail;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderDetails;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderListItem;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderStatusChange;

/**
 * 지점 발주 등록/조회/승인/반려/취소/배정/보류/재개/부분 출고 종결 유스케이스.
 * POST, GET /api/v1/orders, GET /api/v1/orders/{orderId}, GET /api/v1/orders/{orderId}/details,
 * PATCH /api/v1/orders/{orderId}/approve, reject, cancel, hold, resume, complete-partial, POST /api/v1/orders/assign
 */
public interface StoreOrderUseCase {

    /** 발주와 항목을 한 트랜잭션으로 등록하고 StatusHistory(null → REQUESTED)를 남긴 뒤 발주 ID를 반환한다. 응답 조립은 조회 유스케이스로 한다. */
    Long registerStoreOrder(StoreOrderRegisterCommand command);

    /** 필터의 지점·창고가 없으면 각 도메인의 404, 요청 시작 일시가 종료 일시보다 늦으면 400. */
    List<StoreOrderListItem> getStoreOrders(StoreOrderSearchCondition condition);

    /** 발주가 없으면 404 STORE_ORDER_NOT_FOUND */
    StoreOrderDetail getStoreOrder(Long storeOrderId);

    /** 발주가 없으면 404 STORE_ORDER_NOT_FOUND */
    StoreOrderDetails getStoreOrderDetails(Long storeOrderId);

    /**
     * REQUESTED → APPROVED. 발주 행을 잠그고 지점·항목 SKU가 활성인지 확인한 뒤 전이하고 StatusHistory를 남긴다.
     * 발주가 없으면 404 STORE_ORDER_NOT_FOUND, REQUESTED가 아니거나 지점·SKU가 비활성이면 409 CONFLICT.
     */
    StoreOrderStatusChange approveStoreOrder(Long storeOrderId, Long changedBy);

    /**
     * REQUESTED → REJECTED, 항목은 모두 CANCELED. 사유 필수(500자 이하)로 400 VALIDATION_ERROR,
     * 발주가 없으면 404, REQUESTED가 아니면 409 CONFLICT.
     */
    StoreOrderStatusChange rejectStoreOrder(StoreOrderRejectCommand command);

    /**
     * 진행 중 발주를 CANCELED로 바꾸고 항목을 모두 CANCELED로 만든다. 승인 이후 취소는 사유 필수(400).
     * 이미 종결된 발주는 409 CONFLICT, 피킹이 시작된 출고가 있으면 409 ORDER_IN_PICKING.
     * 승인 이후 취소는 출고 연동 포트로 할당 해제·READY 출고 취소를 같은 트랜잭션에서 처리한다.
     */
    StoreOrderCancelResult cancelStoreOrder(StoreOrderCancelCommand command);

    /**
     * APPROVED → ASSIGNED(최초 배정), ASSIGNED의 창고 변경(재배정). 재배정은 사유 필수(400)이고
     * 같은 창고로는 409 CONFLICT, 재고 할당·취소되지 않은 출고가 남아 있으면 409 ORDER_IN_FULFILLMENT.
     * 발주·창고가 없으면 404, 창고가 비활성이거나 상태가 APPROVED·ASSIGNED가 아니면(ON_HOLD 포함) 409 CONFLICT.
     */
    StoreOrderAssignResult assignStoreOrder(StoreOrderAssignCommand command);

    /**
     * ASSIGNED → ON_HOLD. 사유 필수(400). ASSIGNED가 아니면 409 CONFLICT,
     * 피킹이 시작된 출고가 있으면 409 ORDER_IN_PICKING.
     */
    StoreOrderStatusChange holdStoreOrder(StoreOrderHoldCommand command);

    /**
     * ON_HOLD → ASSIGNED. 사유 필수(400). ON_HOLD가 아니거나 배정 창고가 비활성이면 409 CONFLICT.
     * 재개 후 statusReason은 null이다.
     */
    StoreOrderStatusChange resumeStoreOrder(StoreOrderResumeCommand command);

    /**
     * ASSIGNED → COMPLETED(부분 출고 종결). 사유 필수(400). ASSIGNED가 아니면 409 CONFLICT,
     * 진행 중 출고가 있으면 409 OUTBOUND_IN_PROGRESS, 부족 수량이 없으면 409 NO_SHORTAGE.
     * 항목 상태는 바꾸지 않고 부족 수량만 계산해 돌려준다.
     */
    StoreOrderCompletePartialResult completePartialStoreOrder(StoreOrderCompletePartialCommand command);
}
