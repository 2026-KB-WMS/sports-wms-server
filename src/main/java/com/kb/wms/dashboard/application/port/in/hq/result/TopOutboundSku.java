package com.kb.wms.dashboard.application.port.in.hq.result;

/**
 * 출고 완료 수량 상위 SKU 한 줄.
 */
public record TopOutboundSku(
        Long skuId,
        String skuCode,
        String skuName,
        Long quantity
) {
}
