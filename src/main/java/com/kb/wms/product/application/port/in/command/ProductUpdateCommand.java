package com.kb.wms.product.application.port.in.command;

/**
 * PATCH /api/v1/products/{productId} 요청. 모든 필드는 선택이며, 값이 있는 필드만 부분 수정한다.
 * productCode는 불변이므로 이 커맨드에 포함하지 않는다.
 */
public record ProductUpdateCommand(
        Long productId,
        String productName,
        String description,
        Long brandId,
        Long categoryId,
        Boolean isActive
) {

    public boolean hasNoChanges() {
        return productName == null && description == null && brandId == null
                && categoryId == null && isActive == null;
    }
}
