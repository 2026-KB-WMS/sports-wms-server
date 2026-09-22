package com.kb.wms.product.application.port.in.command;

public record ProductRegisterCommand(
        Long brandId,
        Long categoryId,
        String productCode,
        String name,
        String description
) {
}
