package com.kb.wms.product.application.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.product.application.port.in.OptionGroupUseCase;
import com.kb.wms.product.application.port.in.command.OptionGroupRegisterCommand;
import com.kb.wms.product.application.port.out.OptionGroupRepository;
import com.kb.wms.product.application.port.out.OptionValueRepository;
import com.kb.wms.product.application.port.out.ProductRepository;
import com.kb.wms.product.application.port.out.ProductSkuRepository;
import com.kb.wms.product.application.port.out.SkuOptionValueRepository;
import com.kb.wms.product.domain.entity.OptionGroup;
import com.kb.wms.product.domain.entity.OptionValue;
import com.kb.wms.product.domain.entity.ProductSku;
import com.kb.wms.product.domain.entity.SkuOptionValue;
import com.kb.wms.product.exception.ProductErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 옵션 그룹은 특정 상품에 종속되지 않는 공용 마스터라서, "상품별 옵션 그룹 조회"는
 * 해당 상품의 SKU들에 실제로 연결된 옵션 값들을 거슬러 올라가 옵션 그룹을 역으로 찾는다.
 * SKU -> SkuOptionValue -> OptionValue -> OptionGroup 순으로 조인한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OptionGroupService implements OptionGroupUseCase {

    private final OptionGroupRepository optionGroupRepository;
    private final OptionValueRepository optionValueRepository;
    private final SkuOptionValueRepository skuOptionValueRepository;
    private final ProductSkuRepository productSkuRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public OptionGroup registerOptionGroup(OptionGroupRegisterCommand command) {
        if (optionGroupRepository.existsByName(command.name())) {
            throw new BusinessException(ProductErrorCode.DUPLICATE_OPTION_GROUP_NAME);
        }
        return optionGroupRepository.save(OptionGroup.register(command.name()));
    }

    @Override
    public List<OptionGroup> getOptionGroupsByProduct(Long productId) {
        if (!productRepository.findById(productId).isPresent()) {
            throw new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND);
        }

        List<ProductSku> skus = productSkuRepository.findAll(productId);

        Set<Long> optionValueIds = new LinkedHashSet<>();
        for (ProductSku sku : skus) {
            for (SkuOptionValue link : skuOptionValueRepository.findBySkuId(sku.getSkuId())) {
                optionValueIds.add(link.getOptionValueId());
            }
        }

        Set<Long> optionGroupIds = new LinkedHashSet<>();
        for (Long optionValueId : optionValueIds) {
            optionValueRepository.findById(optionValueId)
                    .map(OptionValue::getOptionGroupId)
                    .ifPresent(optionGroupIds::add);
        }

        return optionGroupIds.stream()
                .map(optionGroupRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }
}
