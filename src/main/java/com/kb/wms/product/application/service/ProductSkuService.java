package com.kb.wms.product.application.service;

import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.product.application.port.in.ProductSkuUseCase;
import com.kb.wms.product.application.port.in.command.ProductSkuRegisterCommand;
import com.kb.wms.product.application.port.in.command.SkuOptionConnectCommand;
import com.kb.wms.product.application.port.in.result.SkuOptionSummary;
import com.kb.wms.product.application.port.in.query.ProductSkuSearchCondition;
import com.kb.wms.product.application.port.out.BrandRepository;
import com.kb.wms.product.application.port.out.CategoryRepository;
import com.kb.wms.product.application.port.out.OptionGroupRepository;
import com.kb.wms.product.application.port.out.OptionValueRepository;
import com.kb.wms.product.application.port.out.ProductRepository;
import com.kb.wms.product.application.port.out.ProductSkuRepository;
import com.kb.wms.product.application.port.out.SkuOptionValueRepository;
import com.kb.wms.product.domain.entity.OptionGroup;
import com.kb.wms.product.domain.entity.OptionValue;
import com.kb.wms.product.domain.entity.Product;
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
    private final OptionGroupRepository optionGroupRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public ProductSku registerSku(ProductSkuRegisterCommand command) {
        Product product = productRepository.findById(command.productId())
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
        if (!product.isActive()) {
            throw new BusinessException(ProductErrorCode.PRODUCT_INACTIVE);
        }
        if (productSkuRepository.existsBySkuCode(command.skuCode())) {
            throw new BusinessException(ProductErrorCode.DUPLICATE_SKU_CODE);
        }
        if (command.barcode() != null && productSkuRepository.existsByBarcode(command.barcode())) {
            throw new BusinessException(ProductErrorCode.DUPLICATE_BARCODE);
        }

        ProductSku sku = ProductSku.register(
                command.productId(), command.skuCode(), command.barcode(), command.name(),
                command.weight(), command.currentPurchasePrice(), command.currentSupplyPrice(),
                command.unit(), command.safetyStockQuantity());
        return productSkuRepository.save(sku);
    }

    @Override
    public List<ProductSku> getSkus(ProductSkuSearchCondition condition) {
        if (condition.productId() != null && productRepository.findById(condition.productId()).isEmpty()) {
            throw new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND);
        }
        if (condition.brandId() != null && brandRepository.findById(condition.brandId()).isEmpty()) {
            throw new BusinessException(ProductErrorCode.BRAND_NOT_FOUND);
        }
        if (condition.categoryId() != null && categoryRepository.findById(condition.categoryId()).isEmpty()) {
            throw new BusinessException(ProductErrorCode.CATEGORY_NOT_FOUND);
        }
        return productSkuRepository.search(condition);
    }

    @Override
    public ProductSku getSku(Long skuId) {
        return productSkuRepository.findById(skuId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.SKU_NOT_FOUND));
    }

    @Override
    @Transactional
    public ProductSku changeSkuStatus(Long skuId, boolean active) {
        ProductSku sku = productSkuRepository.findById(skuId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.SKU_NOT_FOUND));
        if (active) {
            Product product = productRepository.findById(sku.getProductId())
                    .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
            if (!product.isActive()) {
                throw new BusinessException(ProductErrorCode.PRODUCT_INACTIVE);
            }
            sku.activate();
        } else {
            sku.deactivate();
        }
        return productSkuRepository.save(sku);
    }

    @Override
    @Transactional
    public void connectOptions(SkuOptionConnectCommand command) {
        ProductSku sku = productSkuRepository.findById(command.skuId())
                .orElseThrow(() -> new BusinessException(ProductErrorCode.SKU_NOT_FOUND));

        Set<Long> connectedOptionGroupIds = new HashSet<>();
        for (SkuOptionValue link : skuOptionValueRepository.findBySkuId(sku.getSkuId())) {
            optionValueRepository.findById(link.getOptionValueId())
                    .map(OptionValue::getOptionGroupId)
                    .ifPresent(connectedOptionGroupIds::add);
        }

        for (Long optionValueId : command.optionValueIds()) {
            OptionValue optionValue = optionValueRepository.findById(optionValueId)
                    .orElseThrow(() -> new BusinessException(ProductErrorCode.OPTION_VALUE_NOT_FOUND));
            if (!optionValue.isActive()) {
                throw new BusinessException(ProductErrorCode.OPTION_VALUE_INACTIVE);
            }
            if (skuOptionValueRepository.existsBySkuIdAndOptionValueId(sku.getSkuId(), optionValueId)) {
                throw new BusinessException(ProductErrorCode.DUPLICATE_OPTION_VALUE);
            }
            if (!connectedOptionGroupIds.add(optionValue.getOptionGroupId())) {
                throw new BusinessException(ProductErrorCode.OPTION_GROUP_CONFLICT);
            }
            skuOptionValueRepository.save(SkuOptionValue.connect(sku.getSkuId(), optionValueId));
        }
    }

    @Override
    public List<SkuOptionSummary> getSkuOptions(Long skuId) {
        List<SkuOptionSummary> summaries = new ArrayList<>();
        for (SkuOptionValue link : skuOptionValueRepository.findBySkuId(skuId)) {
            OptionValue optionValue = optionValueRepository.findById(link.getOptionValueId()).orElse(null);
            if (optionValue == null) {
                continue;
            }
            OptionGroup optionGroup = optionGroupRepository.findById(optionValue.getOptionGroupId()).orElse(null);
            summaries.add(new SkuOptionSummary(
                    optionValue.getOptionGroupId(),
                    optionGroup != null ? optionGroup.getName() : null,
                    optionValue.getOptionValueId(),
                    optionValue.getValue()));
        }
        return summaries;
    }
}
