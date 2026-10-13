package com.kb.wms.product.application.port.in.command;

public record OptionValueRegisterCommand(
        Long optionGroupId,
        String optionCode,
        String value,
        int sortOrder
) {
}
