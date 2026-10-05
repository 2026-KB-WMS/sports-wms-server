package com.kb.wms.outbound.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.kb.wms.outbound.domain.entity.Outbound;
import com.kb.wms.outbound.domain.entity.OutboundLine;
import com.kb.wms.outbound.domain.enums.OutboundStatus;

/**
 * 출고(헤더 + 항목) 영속성 아웃바운드 포트. 헤더와 항목은 한 트랜잭션에서 함께 저장한다.
 */
public interface OutboundRepository {

    /** 새 출고를 저장하거나 ID가 있으면 변경(상태·배송 일시)을 반영한다. */
    Outbound save(Outbound outbound);

    /** 저장된 출고 ID로 항목을 연결해 저장한다. 항목의 outboundId는 호출 측이 채워 넘긴다. 피킹 확정 반영에도 쓴다. */
    List<OutboundLine> saveLines(List<OutboundLine> lines);

    Optional<Outbound> findById(Long outboundId);

    /** 비관적 쓰기 락으로 출고 헤더를 조회한다. 같은 출고에 대한 동시 처리 중 하나만 성공하게 한다. */
    Optional<Outbound> findByIdForUpdate(Long outboundId);

    /** 항목 ID 오름차순 */
    List<OutboundLine> findLinesByOutboundId(Long outboundId);

    /** 발주에 딸린 출고를 생성 순으로 나열한다. 취소된 출고도 포함한다. */
    List<Outbound> findByStoreOrderId(Long storeOrderId);

    /** 여러 발주의 출고를 생성 순으로 나열한다(목록용). */
    List<Outbound> findByStoreOrderIds(Collection<Long> storeOrderIds);

    /** 발주의 해당 상태 출고를 잠그고 조회한다(출고 ID 오름차순). 발주 취소에 따른 READY 출고 일괄 취소에서 쓴다. */
    List<Outbound> findByStoreOrderIdAndStatusForUpdate(Long storeOrderId, OutboundStatus status);

    boolean existsByStoreOrderIdAndStatusIn(Long storeOrderId, Collection<OutboundStatus> statuses);

    boolean existsNotCanceledByStoreOrderId(Long storeOrderId);

    boolean existsByOutboundNo(String outboundNo);

    /** 해당 접두사(예: OB-20261005-)로 시작하는 출고 번호 개수. 일련번호 채번에 쓴다. */
    long countByOutboundNoPrefix(String prefix);
}
