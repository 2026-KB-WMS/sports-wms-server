package com.kb.wms.storeorder.application.port.out;

import java.util.List;
import java.util.Optional;

import com.kb.wms.storeorder.application.port.in.query.StoreOrderSearchCondition;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderLineView;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderSummary;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderView;

/**
 * 지점 발주 조회 전용 아웃바운드 포트.
 *
 * <p>발주에 지점(store)·창고(warehouse)·SKU(product_sku)를 ID로 조인해 이름과 합계를 DB에서 한 번에 만든다(ADR-007).
 * 다른 도메인 테이블은 읽기에만 쓰고 변경하지 않는다. 창고는 배정 전에는 없으므로 left join이다.
 * 페이지네이션은 공통 페이징 도입 시 추가한다(현재는 전체 목록).
 */
public interface StoreOrderQueryRepository {

    /** 요청 일시 내림차순, 발주 ID 내림차순. 조건이 null이면 해당 조건은 무시한다. */
    List<StoreOrderSummary> search(StoreOrderSearchCondition condition);

    Optional<StoreOrderView> findView(Long storeOrderId);

    /** SKU 코드 오름차순 */
    List<StoreOrderLineView> findLineViews(Long storeOrderId);
}
