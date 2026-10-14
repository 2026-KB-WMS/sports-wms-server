package com.kb.wms.warehouse.application.port.in.result;

/**
 * 다른 도메인이 구역의 소속 창고·유형·활성 여부를 확인할 때 쓰는 읽기용 값. 구역 유형은 enum 이름 문자열이다.
 */
public record WarehouseSectionView(
        Long sectionId,
        Long warehouseId,
        String sectionType,
        boolean active
) {
}
