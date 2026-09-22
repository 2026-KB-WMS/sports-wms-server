package com.kb.wms.product.application.port.in.command;

import java.util.List;

public record SkuOptionConnectCommand(
        Long skuId,
        List<Long> optionValueIds
) {
}
