package com.kb.wms.inventory.application.port.out;

import java.util.Collection;

/**
 * 구역 사용 용량 아웃바운드 포트(창고 도메인). 재고 보유 수량이 바뀐 만큼 같은 트랜잭션에서 증감한다.
 *
 * <p>잠금 순서: 구역 행({@link #lock}, section_id 오름차순) → 재고 행(inventory_lot_id 오름차순).
 */
public interface SectionCapacityPort {

    /** 구역 행들을 section_id 오름차순으로 잠근다. 재고 행을 잠그기 전에 호출한다. */
    void lock(Collection<Long> sectionIds);

    /** 수용량을 넘으면 SECTION_CAPACITY_EXCEEDED */
    void occupy(Long sectionId, long quantity);

    void vacate(Long sectionId, long quantity);
}
