package com.kb.wms.inventory.application.port.out;

/**
 * 구역 사용 용량 아웃바운드 포트(창고 도메인). 재고 보유 수량이 바뀐 만큼 같은 트랜잭션에서 증감한다.
 */
public interface SectionCapacityPort {

    /** 수용량을 넘으면 SECTION_CAPACITY_EXCEEDED */
    void occupy(Long sectionId, long quantity);

    void vacate(Long sectionId, long quantity);
}
