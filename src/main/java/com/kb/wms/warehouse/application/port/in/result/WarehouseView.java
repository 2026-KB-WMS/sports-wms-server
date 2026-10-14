package com.kb.wms.warehouse.application.port.in.result;

/**
 * 다른 도메인이 창고의 이름과 활성 여부를 확인할 때 쓰는 읽기용 값. 엔티티를 밖으로 내보내지 않는다.
 */
public record WarehouseView(
        Long warehouseId,
        String name,
        boolean active
) {
}
