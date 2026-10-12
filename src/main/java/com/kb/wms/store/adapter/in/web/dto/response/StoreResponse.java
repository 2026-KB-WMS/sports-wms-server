package com.kb.wms.store.adapter.in.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.kb.wms.store.domain.entity.Store;

/**
 * POST, GET, PATCH /api/v1/stores/{storeId}(및 등록/수정/비활성화) 응답.
 */
public record StoreResponse(
        Long storeId,
        String storeCode,
        String storeName,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String contactName,
        String contactNumber,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static StoreResponse from(Store store) {
        return new StoreResponse(
                store.getStoreId(),
                store.getStoreCode(),
                store.getName(),
                store.getAddress(),
                store.getLatitude(),
                store.getLongitude(),
                store.getContactName(),
                store.getContactNumber(),
                store.isActive(),
                store.getCreatedAt(),
                store.getUpdatedAt());
    }
}
