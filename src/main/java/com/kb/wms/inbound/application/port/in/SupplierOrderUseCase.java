package com.kb.wms.inbound.application.port.in;

import com.kb.wms.common.paging.PageCondition;
import com.kb.wms.common.paging.PageResult;
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.inbound.application.port.in.query.SupplierOrderSearchCondition;
import com.kb.wms.inbound.application.port.in.result.SupplierOrderDetails;
import com.kb.wms.inbound.application.port.in.result.SupplierOrderSummary;

/**
 * 공급처 발주서 조회 유스케이스. GET /api/v1/supplier-orders, GET .../{supplierOrderId}
 * 발주서는 발주 확정으로만 만들어지고 수동 변경 API가 없다. 창고 관리자는 담당 창고의 발주서만 볼 수 있다(그 외 403).
 */
public interface SupplierOrderUseCase {

    PageResult<SupplierOrderSummary> getSupplierOrders(SupplierOrderSearchCondition condition, PageCondition page,
                                                       AuthenticatedUser actor);

    SupplierOrderDetails getSupplierOrder(Long supplierOrderId, AuthenticatedUser actor);
}
