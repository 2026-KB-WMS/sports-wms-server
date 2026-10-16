package com.kb.wms.product.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.common.paging.PageCondition;
import com.kb.wms.common.paging.PageResult;
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.product.application.port.in.ProductUseCase;
import com.kb.wms.product.application.port.in.query.ProductSearchCondition;
import com.kb.wms.product.application.port.in.command.ProductRegisterCommand;
import com.kb.wms.product.application.port.in.command.ProductUpdateCommand;
import com.kb.wms.product.application.port.out.BrandRepository;
import com.kb.wms.product.application.port.out.CategoryRepository;
import com.kb.wms.product.application.port.in.result.ProductSpecSummary;
import com.kb.wms.product.application.port.out.ProductRepository;
import com.kb.wms.product.application.port.out.ProductSpecRepository;
import com.kb.wms.product.application.port.out.ProductSkuRepository;
import com.kb.wms.product.application.port.out.ProductUsagePort;
import com.kb.wms.product.domain.entity.Brand;
import com.kb.wms.product.domain.entity.Category;
import com.kb.wms.product.domain.entity.Product;
import com.kb.wms.product.domain.entity.OptionValue;
import com.kb.wms.product.domain.entity.ProductSku;
import com.kb.wms.product.domain.entity.ProductSpec;
import com.kb.wms.product.domain.enums.OptionType;
import com.kb.wms.product.exception.ProductErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService implements ProductUseCase {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ProductSkuRepository productSkuRepository;
    private final ProductUsagePort productUsagePort;
    private final ProductSpecRepository productSpecRepository;
    private final CategoryOptionResolver categoryOptionResolver;
    private final OptionSelectionResolver optionSelectionResolver;
    private final ProductCodeIssuer productCodeIssuer;

    @Override
    @Transactional
    public Product registerProduct(ProductRegisterCommand command) {
        Brand brand = brandRepository.findById(command.brandId())
                .orElseThrow(() -> new BusinessException(ProductErrorCode.BRAND_NOT_FOUND));
        if (!brand.isActive()) {
            throw new BusinessException(ProductErrorCode.BRAND_INACTIVE);
        }
        Category category = categoryRepository.findById(command.categoryId())
                .orElseThrow(() -> new BusinessException(ProductErrorCode.CATEGORY_NOT_FOUND));
        if (!category.isActive()) {
            throw new BusinessException(ProductErrorCode.CATEGORY_INACTIVE);
        }
        List<OptionValue> specs = optionSelectionResolver.resolve(command.specOptionValueIds(),
                categoryOptionResolver.effectiveMappings(command.categoryId()), OptionType.SPEC);

        Product product = Product.register(
                command.brandId(), command.categoryId(), productCodeIssuer.issue(brand, category),
                command.name(), command.description());
        Product saved = productRepository.save(product);
        if (!specs.isEmpty()) {
            productSpecRepository.replaceAll(saved.getProductId(),
                    specs.stream().map(OptionValue::getOptionValueId).toList());
        }
        return saved;
    }

    @Override
    public PageResult<Product> getProducts(ProductSearchCondition condition, PageCondition page) {
        if (condition.brandId() != null && brandRepository.findById(condition.brandId()).isEmpty()) {
            throw new BusinessException(ProductErrorCode.BRAND_NOT_FOUND);
        }
        if (condition.categoryId() != null && categoryRepository.findById(condition.categoryId()).isEmpty()) {
            throw new BusinessException(ProductErrorCode.CATEGORY_NOT_FOUND);
        }
        return productRepository.search(condition, page);
    }

    @Override
    @Transactional
    public Product updateProduct(ProductUpdateCommand command) {
        if (command.hasNoChanges()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "수정할 필드를 하나 이상 입력해주세요.");
        }

        Product product = productRepository.findById(command.productId())
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        Brand newBrand = null;
        Category newCategory = null;
        boolean brandChanging = false;
        if (command.brandId() != null) {
            Brand brand = brandRepository.findById(command.brandId())
                    .orElseThrow(() -> new BusinessException(ProductErrorCode.BRAND_NOT_FOUND));
            if (!brand.isActive()) {
                throw new BusinessException(ProductErrorCode.BRAND_INACTIVE);
            }
            brandChanging = !command.brandId().equals(product.getBrandId());
            // SKU 코드가 상품 코드에서 만들어지므로, SKU가 있으면 브랜드를 바꿔 코드를 다시 발급하지 못한다.
            if (brandChanging && !productSkuRepository.findAll(product.getProductId()).isEmpty()) {
                throw new BusinessException(ProductErrorCode.PRODUCT_BRAND_LOCKED);
            }
            newBrand = brand;
            product.changeBrand(command.brandId());
        }
        Long targetCategoryId = product.getCategoryId();
        boolean categoryChanging = false;
        if (command.categoryId() != null) {
            Category category = categoryRepository.findById(command.categoryId())
                    .orElseThrow(() -> new BusinessException(ProductErrorCode.CATEGORY_NOT_FOUND));
            if (!category.isActive()) {
                throw new BusinessException(ProductErrorCode.CATEGORY_INACTIVE);
            }
            newCategory = category;
            categoryChanging = !command.categoryId().equals(product.getCategoryId());
            // 스펙과 SKU 옵션은 카테고리의 옵션 그룹 지정에 맞춰 만들어지므로, 이미 있으면 카테고리를 바꾸지 못한다.
            if (categoryChanging && (productSpecRepository.existsByProductId(product.getProductId())
                    || !productSkuRepository.findAll(product.getProductId()).isEmpty())) {
                throw new BusinessException(ProductErrorCode.PRODUCT_CATEGORY_LOCKED);
            }
            targetCategoryId = command.categoryId();
            product.changeCategory(command.categoryId());
        }
        // 카테고리를 바꾸는 경우에도 새 카테고리의 SPEC 그룹을 채우도록 다시 검증한다.
        if (categoryChanging || command.specOptionValueIds() != null) {
            List<OptionValue> specs = optionSelectionResolver.resolve(command.specOptionValueIds(),
                    categoryOptionResolver.effectiveMappings(targetCategoryId), OptionType.SPEC);
            productSpecRepository.replaceAll(product.getProductId(),
                    specs.stream().map(OptionValue::getOptionValueId).toList());
        }
        // 상품 코드에 브랜드·카테고리 코드가 들어 있으므로 둘 중 하나가 바뀌면 새 조합의 다음 번호로 다시 발급한다.
        if (brandChanging || categoryChanging) {
            Brand codeBrand = newBrand != null ? newBrand : brandRepository.findById(product.getBrandId())
                    .orElseThrow(() -> new BusinessException(ProductErrorCode.BRAND_NOT_FOUND));
            Category codeCategory = newCategory != null ? newCategory
                    : categoryRepository.findById(product.getCategoryId())
                            .orElseThrow(() -> new BusinessException(ProductErrorCode.CATEGORY_NOT_FOUND));
            product.changeProductCode(productCodeIssuer.issue(codeBrand, codeCategory));
        }
        if (command.productName() != null) {
            product.changeName(command.productName());
        }
        if (command.description() != null) {
            product.changeDescription(command.description());
        }
        if (command.isActive() != null) {
            if (command.isActive()) {
                product.activate();
            } else {
                // 이미 비활성인 상품을 다시 비활성화하는 요청은 기존처럼 그대로 통과시키고, 활성 상품에만 사용 중 검사를 한다.
                if (product.isActive() && isInUse(command.productId())) {
                    throw new BusinessException(ProductErrorCode.PRODUCT_IN_USE);
                }
                product.deactivate();
                // 재고 도메인은 sku.status만 신뢰하므로 하위 SKU도 같은 트랜잭션에서 비활성화한다.
                productSkuRepository.findAll(command.productId()).stream()
                        .filter(ProductSku::isActive)
                        .forEach(sku -> {
                            sku.deactivate();
                            productSkuRepository.save(sku);
                        });
            }
        }

        return productRepository.save(product);
    }

    @Override
    public Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
    }

    /** 점주에게는 판매 가능한(활성) 상품만 보이므로, isActive 필터가 무엇이든 활성 상품으로 고정한다. */
    @Override
    public PageResult<Product> getProducts(ProductSearchCondition condition, PageCondition page,
                                           AuthenticatedUser actor) {
        if (actor.isStoreOwner()) {
            return getProducts(new ProductSearchCondition(
                    condition.brandId(), condition.categoryId(), condition.keyword(), true), page);
        }
        return getProducts(condition, page);
    }

    /** 점주에게 비활성 상품은 없는 것으로 보인다(404). */
    @Override
    public Product getProduct(Long productId, AuthenticatedUser actor) {
        Product product = getProduct(productId);
        if (actor.isStoreOwner() && !product.isActive()) {
            throw new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND);
        }
        return product;
    }

    @Override
    public List<ProductSpecSummary> getProductSpecs(Long productId) {
        List<Long> optionValueIds = productSpecRepository.findByProductId(productId).stream()
                .map(ProductSpec::getOptionValueId)
                .toList();
        if (optionValueIds.isEmpty()) {
            return List.of();
        }
        return optionSelectionResolver.describe(optionValueIds);
    }

    private boolean isInUse(Long productId) {
        return productUsagePort.hasStock(productId)
                || productUsagePort.hasInProgressInbounds(productId)
                || productUsagePort.hasInProgressPurchaseOrders(productId)
                || productUsagePort.hasInProgressOutbounds(productId)
                || productUsagePort.hasInProgressStoreOrders(productId);
    }
}
