package com.kb.wms.product.application.port.in.command;

import java.util.List;

public record ProductRegisterCommand(
        Long brandId,
        Long categoryId,
        String name,
        String description,
        List<Long> specOptionValueIds
) {
}
