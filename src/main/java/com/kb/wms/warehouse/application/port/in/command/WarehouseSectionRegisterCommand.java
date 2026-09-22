package com.kb.wms.warehouse.application.port.in.command;

import java.math.BigDecimal;

public record WarehouseSectionRegisterCommand(
        Long warehouseId,
        Long parentSectionId,
        String sectionCode,
        String name,
        String sectionType,
        BigDecimal capacity
) {
}
