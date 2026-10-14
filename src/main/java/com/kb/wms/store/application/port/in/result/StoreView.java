package com.kb.wms.store.application.port.in.result;

/**
 * 다른 도메인이 지점의 활성 여부를 확인할 때 쓰는 읽기용 값. 엔티티를 밖으로 내보내지 않는다.
 */
public record StoreView(
        Long storeId,
        boolean active
) {
}
