package com.kb.wms.product.application.port.in.command;

public record BrandRegisterCommand(
        String name,
        String description
) {
}
