package com.kb.wms.inbound.adapter.out.product;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.inbound.application.port.out.SkuPurchasePricePort;
import com.kb.wms.inbound.exception.PurchaseOrderErrorCode;
import com.kb.wms.product.application.port.in.ProductSkuUseCase;
import com.kb.wms.product.application.port.in.result.ProductSkuView;

import lombok.RequiredArgsConstructor;

/**
 * 입고 도메인의 SKU 매입 단가 포트를 상품 도메인 유스케이스에 연결한다.
 */
@Component
@RequiredArgsConstructor
public class SkuPurchasePriceAdapter implements SkuPurchasePricePort {

    private final ProductSkuUseCase productSkuUseCase;

    @Override
    public BigDecimal getPurchasablePrice(Long skuId) {
        ProductSkuView sku = productSkuUseCase.getSkuView(skuId);
        if (!sku.active()) {
            throw new BusinessException(ErrorCode.CONFLICT, "비활성 SKU는 발주할 수 없습니다. skuId=" + skuId);
        }
        if (sku.currentPurchasePrice() == null) {
            throw new BusinessException(PurchaseOrderErrorCode.PURCHASE_PRICE_MISSING);
        }
        return sku.currentPurchasePrice();
    }
}
