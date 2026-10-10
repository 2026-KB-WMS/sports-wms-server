package com.kb.wms.product.application.port.in.command;

/**
 * PATCH /api/v1/products/categories/{categoryId} 요청. 모든 필드는 선택이며, 값이 있는 필드만 부분 수정한다.
 * categoryCode와 상위 카테고리는 불변이므로 이 커맨드에 포함하지 않는다.
 */
public record CategoryUpdateCommand(
        Long categoryId,
        String name,
        Integer sortOrder
) {

    public boolean hasNoChanges() {
        return name == null && sortOrder == null;
    }
}
