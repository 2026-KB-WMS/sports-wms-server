package com.kb.wms.product.application.port.in.command;

public record OptionValueRegisterCommand(
        Long optionGroupId,
        String value,
        int sortOrder
) {
}
