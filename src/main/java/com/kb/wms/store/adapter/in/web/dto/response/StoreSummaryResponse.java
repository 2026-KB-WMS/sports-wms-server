package com.kb.wms.store.adapter.in.web.dto.response;

import java.math.BigDecimal;

import com.kb.wms.store.domain.entity.Store;

/**
 * GET /api/v1/stores (전체 지점 조회) 목록 항목 응답.
 */
public record StoreSummaryResponse(
        Long storeId,
        String storeCode,
        String storeName,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String contactName,
        String contactNumber,
        boolean isActive
) {

    public static StoreSummaryResponse from(Store store) {
        return new StoreSummaryResponse(
                store.getStoreId(),
                store.getStoreCode(),
                store.getName(),
                store.getAddress(),
                store.getLatitude(),
                store.getLongitude(),
                store.getContactName(),
                store.getContactNumber(),
                store.isActive());
    }
}
