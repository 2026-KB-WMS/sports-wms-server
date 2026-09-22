package com.kb.wms.product.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.product.application.port.in.ProductSkuUseCase;
import com.kb.wms.product.application.port.in.command.ProductSkuRegisterCommand;
import com.kb.wms.product.application.port.in.command.SkuOptionConnectCommand;
import com.kb.wms.product.application.port.out.OptionValueRepository;
import com.kb.wms.product.application.port.out.ProductRepository;
import com.kb.wms.product.application.port.out.ProductSkuRepository;
import com.kb.wms.product.application.port.out.SkuOptionValueRepository;
import com.kb.wms.product.domain.entity.ProductSku;
import com.kb.wms.product.domain.entity.SkuOptionValue;
import com.kb.wms.product.exception.ProductErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 역할(HQ_ADMIN/WAREHOUSE_MANAGER/STORE_OWNER)별 응답 필드·목록 차등 처리는
 * 여기서 다루지 않는다. 이 서비스는 항상 전체 데이터를 반환하고,
 * 점주 응답에서 매입 단가·안전 재고를 숨기거나 비활성 SKU를 제외하는 건
 * 웹 어댑터(#20)에서 {@link ProductSku#isPurchaseInfoVisibleTo(boolean)}와
 * {@link ProductSku#isActive()}를 사용해 DTO로 변환할 때 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductSkuService implements ProductSkuUseCase {

    private final ProductSkuRepository productSkuRepository;
    private final ProductRepository productRepository;
    private final OptionValueRepository optionValueRepository;
    private final SkuOptionValueRepository skuOptionValueRepository;

    @Override
    @Transactional
    public ProductSku registerSku(ProductSkuRegisterCommand command) {
        if (!productRepository.findById(command.productId()).isPresent()) {
            throw new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND);
        }
        if (productSkuRepository.existsBySkuCode(command.skuCode())) {
            throw new BusinessException(ProductErrorCode.SKU_CODE_DUPLICATED);
        }
        if (command.barcode() != null && productSkuRepository.existsByBarcode(command.barcode())) {
            throw new BusinessException(ProductErrorCode.BARCODE_DUPLICATED);
        }

        ProductSku sku = ProductSku.register(
                command.productId(), command.skuCode(), command.barcode(), command.name(),
                command.weight(), command.currentPurchasePrice(), command.currentSupplyPrice(),
                command.unit(), command.safetyStockQuantity());
        return productSkuRepository.save(sku);
    }

    @Override
    public List<ProductSku> getSkus(Long productId) {
        return productSkuRepository.findAll(productId);
    }

    @Override
    public ProductSku getSku(Long skuId) {
        return productSkuRepository.findById(skuId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.SKU_NOT_FOUND));
    }

    @Override
    @Transactional
    public void connectOptions(SkuOptionConnectCommand command) {
        ProductSku sku = productSkuRepository.findById(command.skuId())
                .orElseThrow(() -> new BusinessException(ProductErrorCode.SKU_NOT_FOUND));

        for (Long optionValueId : command.optionValueIds()) {
            if (!optionValueRepository.findById(optionValueId).isPresent()) {
                throw new BusinessException(ProductErrorCode.OPTION_VALUE_NOT_FOUND);
            }
            if (skuOptionValueRepository.existsBySkuIdAndOptionValueId(sku.getSkuId(), optionValueId)) {
                throw new BusinessException(ProductErrorCode.DUPLICATE_OPTION_VALUE);
            }
            skuOptionValueRepository.save(SkuOptionValue.connect(sku.getSkuId(), optionValueId));
        }
    }
}
