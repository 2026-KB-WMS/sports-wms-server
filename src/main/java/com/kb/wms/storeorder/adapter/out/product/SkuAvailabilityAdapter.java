package com.kb.wms.storeorder.adapter.out.product;

import org.springframework.stereotype.Component;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.product.application.port.in.ProductSkuUseCase;
import com.kb.wms.storeorder.application.port.out.SkuAvailabilityPort;

import lombok.RequiredArgsConstructor;

/**
 * 지점 발주 도메인의 SKU 상태 포트를 상품 도메인 유스케이스에 연결한다.
 */
@Component
@RequiredArgsConstructor
public class SkuAvailabilityAdapter implements SkuAvailabilityPort {

    private final ProductSkuUseCase productSkuUseCase;

    @Override
    public void requireActive(Long skuId) {
        if (!productSkuUseCase.getSku(skuId).isActive()) {
            throw new BusinessException(ErrorCode.CONFLICT, "비활성 SKU가 포함된 발주는 승인할 수 없습니다. skuId=" + skuId);
        }
    }
}
