package com.kb.wms.product.application.port.in.command;

public record CategoryRegisterCommand(
        Long parentCategoryId,
        String categoryCode,
        String name,
        int sortOrder
) {
}
