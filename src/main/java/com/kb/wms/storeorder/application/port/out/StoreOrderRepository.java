package com.kb.wms.storeorder.application.port.out;

import java.util.List;
import java.util.Optional;

import com.kb.wms.storeorder.domain.entity.StoreOrder;
import com.kb.wms.storeorder.domain.entity.StoreOrderLine;

/**
 * 지점 발주(헤더 + 항목) 영속성 아웃바운드 포트. 헤더와 항목은 한 트랜잭션에서 함께 저장한다.
 */
public interface StoreOrderRepository {

    /** 새 발주를 저장하거나 ID가 있으면 변경(상태·창고)을 반영한다. */
    StoreOrder save(StoreOrder storeOrder);

    /** 저장된 발주 헤더 ID로 항목을 연결해 저장한다. 항목의 storeOrderId는 호출 측이 채워 넘긴다. 항목 상태 변경 반영에도 쓴다. */
    List<StoreOrderLine> saveLines(List<StoreOrderLine> lines);

    Optional<StoreOrder> findById(Long storeOrderId);

    /**
     * 비관적 쓰기 락으로 발주 헤더를 조회한다. 승인·반려·취소·배정처럼 발주 상태를 확인하고 바꾸는 흐름에서 쓰며,
     * 같은 발주에 대한 동시 처리 중 하나만 성공하게 한다.
     */
    Optional<StoreOrder> findByIdForUpdate(Long storeOrderId);

    /** 항목 ID 오름차순 */
    List<StoreOrderLine> findLinesByStoreOrderId(Long storeOrderId);

    /** 발주 항목을 비관적 쓰기 락으로 조회한다(항목 ID 오름차순). 취소·반려에서 항목 상태를 바꿀 때 쓴다. */
    List<StoreOrderLine> findLinesByStoreOrderIdForUpdate(Long storeOrderId);

    boolean existsByOrderNo(String orderNo);

    /** 해당 접두사(예: SO-20261004-)로 시작하는 주문 번호 개수. 일련번호 채번에 쓴다. */
    long countByOrderNoPrefix(String prefix);
}
