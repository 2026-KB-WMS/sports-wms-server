package com.kb.wms.outbound.adapter.out.product;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kb.wms.outbound.application.port.out.SkuSupplyPricePort;
import com.kb.wms.product.application.port.in.ProductSkuUseCase;

import lombok.RequiredArgsConstructor;

/** {@link SkuSupplyPricePort}를 상품 도메인의 인바운드 유스케이스로 구현한다. */
@Component
@RequiredArgsConstructor
public class OutboundSkuSupplyPriceAdapter implements SkuSupplyPricePort {

    private final ProductSkuUseCase productSkuUseCase;

    @Override
    public Optional<BigDecimal> findCurrentSupplyPrice(Long skuId) {
        return Optional.ofNullable(productSkuUseCase.getSku(skuId).getCurrentSupplyPrice());
    }
}
