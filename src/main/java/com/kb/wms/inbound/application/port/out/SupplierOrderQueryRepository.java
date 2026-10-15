package com.kb.wms.inbound.application.port.out;

import java.util.List;
import java.util.Optional;

import com.kb.wms.inbound.application.port.in.query.SupplierOrderSearchCondition;
import com.kb.wms.inbound.application.port.in.result.SupplierOrderLineView;
import com.kb.wms.inbound.application.port.in.result.SupplierOrderSummary;
import com.kb.wms.inbound.application.port.in.result.SupplierOrderView;

/**
 * 공급처 발주서 조회 전용 아웃바운드 포트.
 *
 * <p>발주서에 요청(purchase_order)·창고(warehouse)·공급처(supplier)·SKU(product_sku)를 ID로 조인해
 * 이름과 합계를 DB에서 한 번에 만든다. 다른 도메인 테이블은 읽기에만 쓴다.
 */
public interface SupplierOrderQueryRepository {

    /** 등록 일시 내림차순, 발주서 ID 내림차순. 조건이 null이면 해당 조건은 무시한다. */
    List<SupplierOrderSummary> search(SupplierOrderSearchCondition condition);

    /** 요청 상세·확정 응답용. 요청에 속한 발주서 목록(발주서 ID 오름차순). */
    List<SupplierOrderSummary> findSummariesByPurchaseOrderId(Long purchaseOrderId);

    Optional<SupplierOrderView> findView(Long supplierOrderId);

    /** SKU 코드 오름차순 */
    List<SupplierOrderLineView> findLineViews(Long supplierOrderId);
}
