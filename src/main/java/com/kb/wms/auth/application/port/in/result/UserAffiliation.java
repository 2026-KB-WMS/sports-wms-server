package com.kb.wms.auth.application.port.in.result;

import java.util.List;

/**
 * 사용자의 창고·지점 소속 ID. 한 사용자가 여러 창고·지점에 배정될 수 있다.
 * 요청마다 인증 주체를 만들 때(ADR-016)와 승인 시 소속 배정 여부를 확인하는 데 쓴다.
 *
 * @param warehouseIds 배정된 창고 ID (오름차순)
 * @param storeIds     배정된 지점 ID (오름차순)
 */
public record UserAffiliation(
        List<Long> warehouseIds,
        List<Long> storeIds
) {

    public UserAffiliation {
        warehouseIds = List.copyOf(warehouseIds);
        storeIds = List.copyOf(storeIds);
    }

    /** 창고 또는 지점 소속이 하나라도 있는가. */
    public boolean hasAny() {
        return !warehouseIds.isEmpty() || !storeIds.isEmpty();
    }
}
