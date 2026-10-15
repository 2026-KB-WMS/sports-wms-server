package com.kb.wms.inbound.application.port.out;

import java.util.List;
import java.util.Optional;

import com.kb.wms.inbound.domain.entity.SupplierOrder;

/**
 * 공급처 발주서 영속성 아웃바운드 포트. 라인 연결(purchase_order_line.supplier_order_id)은
 * {@link PurchaseOrderRepository}가 라인을 저장하며 반영한다.
 */
public interface SupplierOrderRepository {

    SupplierOrder save(SupplierOrder supplierOrder);

    Optional<SupplierOrder> findById(Long supplierOrderId);

    /** 비관적 쓰기 락으로 발주서를 조회한다. 입고 등록·완료 전파가 서로 어긋나지 않게 한다. */
    Optional<SupplierOrder> findByIdForUpdate(Long supplierOrderId);

    /** 요청에 속한 발주서(발주서 ID 오름차순). */
    List<SupplierOrder> findByPurchaseOrderId(Long purchaseOrderId);

    /** 요청에 속한 발주서를 비관적 쓰기 락으로 조회한다(발주서 ID 오름차순). 요청 취소·완료 전파에서 쓴다. */
    List<SupplierOrder> findByPurchaseOrderIdForUpdate(Long purchaseOrderId);

    boolean existsBySupplierOrderNo(String supplierOrderNo);

    /** 해당 접두어(예: SO-20261015-)로 시작하는 발주서 번호 개수. 일련번호 채번에 쓴다. */
    long countBySupplierOrderNoPrefix(String prefix);

    /** 공급처에 진행 중인 발주서(ISSUED·PARTIALLY_RECEIVED)가 있는지. 공급처 비활성화 가드(SUPPLIER_IN_USE)에 쓴다. */
    boolean existsInProgressBySupplierId(Long supplierId);
}
