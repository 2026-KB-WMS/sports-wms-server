package com.kb.wms.inbound.adapter.out.product;

import org.springframework.stereotype.Component;

import com.kb.wms.inbound.application.port.out.SkuLookupPort;
import com.kb.wms.product.application.port.in.ProductSkuUseCase;

import lombok.RequiredArgsConstructor;

/**
 * 입고 도메인의 SKU 조회 포트를 상품 도메인 유스케이스에 연결한다.
 */
@Component
@RequiredArgsConstructor
public class SkuLookupAdapter implements SkuLookupPort {

    private final ProductSkuUseCase productSkuUseCase;

    @Override
    public void requireExists(Long skuId) {
        productSkuUseCase.getSkuView(skuId);
    }
}
