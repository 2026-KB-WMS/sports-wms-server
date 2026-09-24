package com.kb.wms.warehouse.application.port.in;

/**
 * 구역 사용 용량(current_capacity) 증감 유스케이스. 재고 수량이 바뀔 때 재고 도메인이 같은 트랜잭션 안에서 호출한다.
 * 구역 행을 비관적 락으로 잠가 동시 적치가 수용량을 넘지 않게 한다.
 */
public interface WarehouseSectionCapacityUseCase {

    /**
     * 수량만큼 사용 용량을 늘린다. 구역이 없으면 SECTION_NOT_FOUND, 수용량을 넘으면 SECTION_CAPACITY_EXCEEDED.
     */
    void occupy(Long sectionId, long quantity);

    /**
     * 수량만큼 사용 용량을 줄인다(0 미만으로는 내려가지 않음). 구역이 없으면 SECTION_NOT_FOUND.
     */
    void vacate(Long sectionId, long quantity);
}
