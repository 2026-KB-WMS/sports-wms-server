package com.kb.wms.product.application.port.in.command;

/**
 * PATCH /api/v1/products/brands/{brandId} 요청. 모든 필드는 선택이며, 값이 있는 필드만 부분 수정한다.
 *
 * @param brandCode 코드가 없는 기존 브랜드에 한해 한 번 지정할 수 있다. 이미 코드가 있으면 거절한다.
 */
public record BrandUpdateCommand(
        Long brandId,
        String name,
        String description,
        String brandCode
) {

    public BrandUpdateCommand(Long brandId, String name, String description) {
        this(brandId, name, description, null);
    }

    public boolean hasNoChanges() {
        return name == null && description == null && brandCode == null;
    }
}
