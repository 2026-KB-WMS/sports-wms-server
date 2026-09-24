package com.kb.wms.warehouse.application.port.in;

import java.util.Collection;

/**
 * 구역 사용 용량(current_capacity) 증감 유스케이스. 재고 수량이 바뀔 때 재고 도메인이 같은 트랜잭션 안에서 호출한다.
 * 구역 행을 비관적 락으로 잠가 동시 적치가 수용량을 넘지 않게 한다.
 *
 * <p>잠금 순서: 재고 수량을 바꾸는 트랜잭션은 {@link #lock}으로 관련 구역 행을 먼저(section_id 오름차순) 잠근 뒤
 * 재고 행을 잠근다. 모든 흐름이 같은 순서를 지켜 교착을 막는다.
 */
public interface WarehouseSectionCapacityUseCase {

    /**
     * 구역 행들을 section_id 오름차순으로 잠근다. 없는 구역이 있으면 SECTION_NOT_FOUND.
     */
    void lock(Collection<Long> sectionIds);

    /**
     * 수량만큼 사용 용량을 늘린다. 구역이 없으면 SECTION_NOT_FOUND, 비활성 구역이면 SECTION_INACTIVE,
     * 비활성 창고의 구역이면 WAREHOUSE_INACTIVE, 수용량을 넘으면 SECTION_CAPACITY_EXCEEDED.
     */
    void occupy(Long sectionId, long quantity);

    /**
     * 수량만큼 사용 용량을 줄인다(0 미만으로는 내려가지 않음). 구역이 없으면 SECTION_NOT_FOUND.
     */
    void vacate(Long sectionId, long quantity);
}
