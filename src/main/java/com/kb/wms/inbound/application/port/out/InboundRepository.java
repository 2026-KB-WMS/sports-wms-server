package com.kb.wms.inbound.application.port.out;

import java.util.List;
import java.util.Optional;

import com.kb.wms.inbound.domain.entity.Inbound;
import com.kb.wms.inbound.domain.entity.InboundLine;

/**
 * 입고(헤더 + 검수 항목) 영속성 아웃바운드 포트. 헤더와 항목은 한 트랜잭션에서 함께 저장한다.
 */
public interface InboundRepository {

    Inbound save(Inbound inbound);

    Optional<Inbound> findById(Long inboundId);

    /** 입고 행을 비관적 락으로 조회한다. 취소·완료·검수가 동시에 들어와도 한쪽만 성공하게 할 때 쓴다. */
    Optional<Inbound> findByIdForUpdate(Long inboundId);

    /** 검수 항목을 저장한다. 항목의 inboundId는 호출 측이 채워 넘긴다. */
    List<InboundLine> saveLines(List<InboundLine> lines);

    List<InboundLine> findLinesByInboundId(Long inboundId);

    /** 검수 호출마다 기존 검수 항목을 통째로 교체하기 위해 입고의 항목을 모두 지운다. */
    void deleteLinesByInboundId(Long inboundId);

    boolean existsByInboundNo(String inboundNo);

    /** 해당 접두어(예: IB-20261002-)로 시작하는 입고 번호 개수. 일련번호 채번에 쓴다. */
    long countByInboundNoPrefix(String prefix);

    /** 발주에 완료되지 않은(ARRIVED·INSPECTING) 입고가 있는지. 새 입고 등록 가능 여부(INBOUND_IN_PROGRESS) 판단 기준이다. */
    boolean existsInProgressByPurchaseOrderId(Long purchaseOrderId);

    /** 발주에 취소되지 않은 입고가 있는지. 발주 취소 가능 여부(PURCHASE_ORDER_HAS_INBOUND) 판단 기준이다. */
    boolean existsNotCanceledByPurchaseOrderId(Long purchaseOrderId);
}
