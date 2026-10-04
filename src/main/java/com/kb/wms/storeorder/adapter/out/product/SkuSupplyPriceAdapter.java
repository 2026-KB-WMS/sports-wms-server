package com.kb.wms.storeorder.adapter.out.product;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.product.application.port.in.ProductSkuUseCase;
import com.kb.wms.product.domain.entity.ProductSku;
import com.kb.wms.storeorder.application.port.out.SkuSupplyPricePort;
import com.kb.wms.storeorder.exception.StoreOrderErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 지점 발주 도메인의 SKU 공급 단가 포트를 상품 도메인 유스케이스에 연결한다.
 */
@Component
@RequiredArgsConstructor
public class SkuSupplyPriceAdapter implements SkuSupplyPricePort {

    private final ProductSkuUseCase productSkuUseCase;

    @Override
    public BigDecimal getOrderableSupplyPrice(Long skuId) {
        ProductSku sku = productSkuUseCase.getSku(skuId);
        if (!sku.isActive()) {
            throw new BusinessException(ErrorCode.CONFLICT, "비활성 SKU는 발주할 수 없습니다. skuId=" + skuId);
        }
        if (sku.getCurrentSupplyPrice() == null) {
            throw new BusinessException(StoreOrderErrorCode.SUPPLY_PRICE_MISSING);
        }
        return sku.getCurrentSupplyPrice();
    }
}
