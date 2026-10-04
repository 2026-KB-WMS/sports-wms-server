package com.kb.wms.storeorder.application.port.in;

import java.util.List;

import com.kb.wms.storeorder.application.port.in.command.StoreOrderRegisterCommand;
import com.kb.wms.storeorder.application.port.in.query.StoreOrderSearchCondition;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderDetail;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderDetails;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderListItem;

/**
 * 지점 발주 등록/조회 유스케이스.
 * POST, GET /api/v1/orders, GET /api/v1/orders/{orderId}, GET /api/v1/orders/{orderId}/details
 * 승인·반려·취소는 #121, 배정·보류·재개·부분 출고 종결은 #122에서 추가한다.
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
}
