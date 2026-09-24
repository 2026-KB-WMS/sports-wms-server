package com.kb.wms.inventory.adapter.out.product;

import org.springframework.stereotype.Component;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.inventory.application.port.out.SkuStatusPort;
import com.kb.wms.inventory.exception.InventoryErrorCode;
import com.kb.wms.product.application.port.in.ProductSkuUseCase;

import lombok.RequiredArgsConstructor;

/**
 * 재고 도메인의 SKU 상태 포트를 상품 도메인 유스케이스로 연결한다.
 */
@Component
@RequiredArgsConstructor
public class SkuStatusAdapter implements SkuStatusPort {

    private final ProductSkuUseCase productSkuUseCase;

    @Override
    public void requireActive(Long skuId) {
        if (!productSkuUseCase.getSku(skuId).isActive()) {
            throw new BusinessException(InventoryErrorCode.SKU_NOT_ACTIVE);
        }
    }
}
