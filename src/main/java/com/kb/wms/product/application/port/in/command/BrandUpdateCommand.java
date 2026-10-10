package com.kb.wms.product.application.port.in.command;

/**
 * PATCH /api/v1/products/brands/{brandId} 요청. 모든 필드는 선택이며, 값이 있는 필드만 부분 수정한다.
 */
public record BrandUpdateCommand(
        Long brandId,
        String name,
        String description
) {

    public boolean hasNoChanges() {
        return name == null && description == null;
    }
}
