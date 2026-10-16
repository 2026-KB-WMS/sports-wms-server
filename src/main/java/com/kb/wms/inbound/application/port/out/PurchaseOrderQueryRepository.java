package com.kb.wms.inbound.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.kb.wms.common.paging.PageCondition;
import com.kb.wms.common.paging.PageResult;
import com.kb.wms.inbound.application.port.in.query.PurchaseOrderSearchCondition;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderLineView;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderSummary;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderSupplierView;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderView;

/**
 * 발주 조회 전용 아웃바운드 포트.
 *
 * <p>발주에 창고(warehouse)·공급처(supplier)·SKU(product_sku)를 ID로 조인해 이름과 합계를 DB에서 한 번에 만든다.
 * 다른 도메인 테이블은 읽기에만 쓰고 변경하지 않는다.
 */
public interface PurchaseOrderQueryRepository {

    /** 페이지 조건의 정렬로 한 페이지를 돌려준다. 조건이 null이면 해당 조건은 무시한다. */
    PageResult<PurchaseOrderSummary> search(PurchaseOrderSearchCondition condition, PageCondition page);

    Optional<PurchaseOrderView> findView(Long purchaseOrderId);

    /** SKU 코드 오름차순 */
    List<PurchaseOrderLineView> findLineViews(Long purchaseOrderId);

    /** 발주별로 라인에 배정된 공급처(발주 ID, 공급처 ID 오름차순). 배정 전 라인은 건너뛴다. */
    List<PurchaseOrderSupplierView> findSuppliers(Collection<Long> purchaseOrderIds);
}
