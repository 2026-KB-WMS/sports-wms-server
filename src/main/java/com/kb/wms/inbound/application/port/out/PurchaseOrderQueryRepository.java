package com.kb.wms.inbound.application.port.out;

import java.util.List;
import java.util.Optional;

import com.kb.wms.inbound.application.port.in.query.PurchaseOrderSearchCondition;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderLineView;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderSummary;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderView;

/**
 * 발주 조회 전용 아웃바운드 포트.
 *
 * <p>발주에 창고(warehouse)·공급처(supplier)·SKU(product_sku)를 ID로 조인해 이름과 합계를 DB에서 한 번에 만든다.
 * 다른 도메인 테이블은 읽기에만 쓰고 변경하지 않는다.
 * 페이지네이션은 공통 페이징 도입 시 추가한다(현재는 전체 목록).
 */
public interface PurchaseOrderQueryRepository {

    /** 등록 일시 내림차순, 발주 ID 내림차순. 조건이 null이면 해당 조건은 무시한다. */
    List<PurchaseOrderSummary> search(PurchaseOrderSearchCondition condition);

    Optional<PurchaseOrderView> findView(Long purchaseOrderId);

    /** SKU 코드 오름차순 */
    List<PurchaseOrderLineView> findLineViews(Long purchaseOrderId);
}
