package com.kb.wms.product.application.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.product.application.port.in.ProductSkuUseCase;
import com.kb.wms.product.application.port.in.command.ProductSkuRegisterCommand;
import com.kb.wms.product.application.port.in.command.ProductSkuUpdateCommand;
import com.kb.wms.product.application.port.in.command.SkuOptionConnectCommand;
import com.kb.wms.product.application.port.in.query.ProductSkuSearchCondition;
import com.kb.wms.product.application.port.in.result.ProductSkuView;
import com.kb.wms.product.application.port.in.result.SkuOptionSummary;
import com.kb.wms.product.application.port.out.BrandRepository;
import com.kb.wms.product.application.port.out.CategoryRepository;
import com.kb.wms.product.application.port.out.OptionGroupRepository;
import com.kb.wms.product.application.port.out.OptionValueRepository;
import com.kb.wms.product.application.port.out.ProductRepository;
import com.kb.wms.product.application.port.out.ProductSkuRepository;
import com.kb.wms.product.application.port.out.ProductUsagePort;
import com.kb.wms.product.application.port.out.SkuOptionValueRepository;
import com.kb.wms.product.application.port.out.SkuPriceHistoryRepository;
import com.kb.wms.product.domain.entity.CategoryOptionGroup;
import com.kb.wms.product.domain.entity.OptionGroup;
import com.kb.wms.product.domain.entity.OptionValue;
import com.kb.wms.product.domain.entity.Product;
import com.kb.wms.product.domain.entity.ProductSku;
import com.kb.wms.product.domain.entity.SkuOptionValue;
import com.kb.wms.product.domain.entity.SkuPriceHistory;
import com.kb.wms.product.domain.enums.OptionType;
import com.kb.wms.product.domain.enums.SkuUnit;
import com.kb.wms.product.exception.ProductErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 점주(STORE_OWNER)에게는 활성 SKU만 보인다. 사용자 요청용 조회(actor를 받는 메서드)가 목록은 활성으로 고정하고
 * 단건은 비활성이면 404로 처리하며, 다른 도메인의 내부 조회(actor 없는 메서드)는 항상 전체 데이터를 반환한다.
 * 점주 응답에서 매입 단가·안전 재고를 숨기는 건 웹 어댑터가
 * {@link ProductSku#isPurchaseInfoVisibleTo(boolean)}로 판단해 DTO로 변환할 때 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductSkuService implements ProductSkuUseCase {

    private static final int SKU_CODE_MAX_LENGTH = 50;
    private static final int SKU_NAME_MAX_LENGTH = 200;

    private final ProductSkuRepository productSkuRepository;
    private final ProductRepository productRepository;
    private final OptionValueRepository optionValueRepository;
    private final SkuOptionValueRepository skuOptionValueRepository;
    private final OptionGroupRepository optionGroupRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ProductUsagePort productUsagePort;
    private final SkuPriceHistoryRepository skuPriceHistoryRepository;
    private final CategoryOptionResolver categoryOptionResolver;
    private final OptionSelectionResolver optionSelectionResolver;

    @Override
    @Transactional
    public ProductSku registerSku(ProductSkuRegisterCommand command) {
        Product product = productRepository.findById(command.productId())
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
        if (!product.isActive()) {
            throw new BusinessException(ProductErrorCode.PRODUCT_INACTIVE);
        }
        if (command.unit() != null && !SkuUnit.isValidCode(command.unit())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "허용되지 않는 수량 단위입니다. 허용 값: " + allowedUnits());
        }

        List<CategoryOptionGroup> mappings = categoryOptionResolver.effectiveMappings(product.getCategoryId());
        List<OptionValue> options = optionSelectionResolver.resolve(command.optionValueIds(), mappings,
                OptionType.SKU);
        assertCombinationAvailable(product.getProductId(), null, options);

        String skuCode = generateSkuCode(product, options);
        String skuName = generateSkuName(product, options);
        if (productSkuRepository.existsBySkuCode(skuCode)) {
            throw new BusinessException(ProductErrorCode.DUPLICATE_SKU_CODE);
        }
        if (command.barcode() != null && productSkuRepository.existsByBarcode(command.barcode())) {
            throw new BusinessException(ProductErrorCode.DUPLICATE_BARCODE);
        }

        ProductSku sku = ProductSku.register(
                command.productId(), skuCode, command.barcode(), skuName,
                command.weight(), command.currentPurchasePrice(), command.currentSupplyPrice(),
                command.unit(), command.safetyStockQuantity());
        ProductSku saved = productSkuRepository.save(sku);
        for (OptionValue option : options) {
            skuOptionValueRepository.save(SkuOptionValue.connect(saved.getSkuId(), option.getOptionValueId()));
        }
        return saved;
    }

    /** 상품 코드 뒤에 옵션 코드를 카테고리 지정 순서대로 `-`로 붙인다. 옵션이 없으면 상품 코드 그대로다. */
    private String generateSkuCode(Product product, List<OptionValue> options) {
        StringBuilder code = new StringBuilder(product.getProductCode());
        for (OptionValue option : options) {
            if (option.getOptionCode() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "옵션 값 '" + option.getValue() + "'에 코드가 없어 SKU 코드를 만들 수 없습니다. "
                                + "코드가 있는 옵션 값을 선택해주세요.");
            }
            code.append('-').append(option.getOptionCode());
        }
        if (code.length() > SKU_CODE_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "만들어진 SKU 코드가 " + SKU_CODE_MAX_LENGTH + "자를 넘습니다. 옵션 코드를 더 짧게 지정해주세요.");
        }
        return code.toString();
    }

    /** `상품명 (옵션값/옵션값)`. 옵션이 없으면 상품명 그대로다. */
    private String generateSkuName(Product product, List<OptionValue> options) {
        String name = options.isEmpty() ? product.getName()
                : product.getName() + " (" + options.stream().map(OptionValue::getValue)
                        .collect(Collectors.joining("/")) + ")";
        if (name.length() > SKU_NAME_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "만들어진 SKU 명칭이 " + SKU_NAME_MAX_LENGTH + "자를 넘습니다. 상품명을 더 짧게 수정해주세요.");
        }
        return name;
    }

    /** 같은 상품의 다른 SKU(비활성 포함)가 같은 옵션 조합을 갖고 있으면 DUPLICATE_SKU_OPTION_COMBINATION. */
    private void assertCombinationAvailable(Long productId, Long excludeSkuId, List<OptionValue> options) {
        Set<Long> combination = options.stream().map(OptionValue::getOptionValueId).collect(Collectors.toSet());
        List<Long> otherSkuIds = productSkuRepository.findAll(productId).stream()
                .map(ProductSku::getSkuId)
                .filter(id -> !id.equals(excludeSkuId))
                .toList();
        if (otherSkuIds.isEmpty()) {
            return;
        }
        Map<Long, Set<Long>> existing = new HashMap<>();
        for (SkuOptionValue link : skuOptionValueRepository.findBySkuIdIn(otherSkuIds)) {
            existing.computeIfAbsent(link.getSkuId(), id -> new HashSet<>()).add(link.getOptionValueId());
        }
        for (Long skuId : otherSkuIds) {
            if (existing.getOrDefault(skuId, Set.of()).equals(combination)) {
                throw new BusinessException(ProductErrorCode.DUPLICATE_SKU_OPTION_COMBINATION);
            }
        }
    }

    private static String allowedUnits() {
        return Arrays.stream(SkuUnit.values()).map(SkuUnit::getCode).collect(Collectors.joining(", "));
    }

    @Override
    @Transactional
    public ProductSku updateSku(ProductSkuUpdateCommand command, AuthenticatedUser actor) {
        if (command.hasNoChanges()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "수정할 필드를 하나 이상 입력해주세요.");
        }
        ProductSku sku = productSkuRepository.findById(command.skuId())
                .orElseThrow(() -> new BusinessException(ProductErrorCode.SKU_NOT_FOUND));
        // 자기 자신의 바코드와 같은 값을 다시 보내는 요청은 중복으로 보지 않는다.
        if (command.barcode() != null && !command.barcode().equals(sku.getBarcode())
                && productSkuRepository.existsByBarcode(command.barcode())) {
            throw new BusinessException(ProductErrorCode.DUPLICATE_BARCODE);
        }

        BigDecimal previousPurchasePrice = sku.getCurrentPurchasePrice();
        BigDecimal previousSupplyPrice = sku.getCurrentSupplyPrice();
        sku.update(command.skuName(), command.barcode(), command.weight(), command.currentPurchasePrice(),
                command.currentSupplyPrice(), command.safetyStockQuantity());
        ProductSku saved = productSkuRepository.save(sku);

        boolean priceChanged = previousPurchasePrice.compareTo(saved.getCurrentPurchasePrice()) != 0
                || previousSupplyPrice.compareTo(saved.getCurrentSupplyPrice()) != 0;
        if (priceChanged) {
            skuPriceHistoryRepository.save(SkuPriceHistory.record(saved.getSkuId(),
                    previousPurchasePrice, saved.getCurrentPurchasePrice(),
                    previousSupplyPrice, saved.getCurrentSupplyPrice(), actor.userId()));
        }
        return saved;
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
    public ProductSkuView getSkuView(Long skuId) {
        ProductSku sku = getSku(skuId);
        return new ProductSkuView(sku.getSkuId(), sku.isActive(), sku.getCurrentPurchasePrice(),
                sku.getCurrentSupplyPrice());
    }

    /** 점주에게는 판매 가능한(활성) SKU만 보이므로, isActive 필터가 무엇이든 활성 SKU로 고정한다. */
    @Override
    public List<ProductSku> getSkus(ProductSkuSearchCondition condition, AuthenticatedUser actor) {
        if (actor.isStoreOwner()) {
            return getSkus(new ProductSkuSearchCondition(condition.productId(), condition.brandId(),
                    condition.categoryId(), condition.keyword(), true));
        }
        return getSkus(condition);
    }

    /** 점주에게 비활성 SKU는 없는 것으로 보인다(404). */
    @Override
    public ProductSku getSku(Long skuId, AuthenticatedUser actor) {
        ProductSku sku = getSku(skuId);
        if (actor.isStoreOwner() && !sku.isActive()) {
            throw new BusinessException(ProductErrorCode.SKU_NOT_FOUND);
        }
        return sku;
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
            // 이미 비활성인 SKU를 다시 비활성화하는 요청은 그대로 통과시키고, 활성 SKU에만 사용 중 검사를 한다.
            if (sku.isActive() && productUsagePort.isSkuInUse(skuId)) {
                throw new BusinessException(ProductErrorCode.SKU_IN_USE);
            }
            sku.deactivate();
        }
        return productSkuRepository.save(sku);
    }

    /** 옵션이 없는 기존 SKU에 한 번만 연결한다. 등록할 때와 같은 검증(카테고리 지정·필수 그룹·조합 중복)을 거친다. */
    @Override
    @Transactional
    public void connectOptions(SkuOptionConnectCommand command) {
        ProductSku sku = productSkuRepository.findById(command.skuId())
                .orElseThrow(() -> new BusinessException(ProductErrorCode.SKU_NOT_FOUND));
        if (!skuOptionValueRepository.findBySkuId(sku.getSkuId()).isEmpty()) {
            throw new BusinessException(ProductErrorCode.SKU_OPTIONS_ALREADY_CONNECTED);
        }
        Product product = productRepository.findById(sku.getProductId())
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        List<OptionValue> options = optionSelectionResolver.resolve(command.optionValueIds(),
                categoryOptionResolver.effectiveMappings(product.getCategoryId()), OptionType.SKU);
        assertCombinationAvailable(product.getProductId(), sku.getSkuId(), options);
        for (OptionValue option : options) {
            skuOptionValueRepository.save(SkuOptionValue.connect(sku.getSkuId(), option.getOptionValueId()));
        }
    }

    @Override
    public List<SkuOptionSummary> getSkuOptions(Long skuId) {
        return getSkuOptionsBySkuIds(List.of(skuId)).getOrDefault(skuId, List.of());
    }

    @Override
    public Map<Long, List<SkuOptionSummary>> getSkuOptionsBySkuIds(Collection<Long> skuIds) {
        List<SkuOptionValue> links = skuOptionValueRepository.findBySkuIdIn(skuIds);

        Map<Long, OptionValue> optionValues = optionValueRepository.findAllByIds(
                        links.stream().map(SkuOptionValue::getOptionValueId).distinct().toList()).stream()
                .collect(Collectors.toMap(OptionValue::getOptionValueId, Function.identity()));
        Map<Long, OptionGroup> optionGroups = optionGroupRepository.findAllByIds(
                        optionValues.values().stream().map(OptionValue::getOptionGroupId).distinct().toList()).stream()
                .collect(Collectors.toMap(OptionGroup::getOptionGroupId, Function.identity()));

        Map<Long, List<SkuOptionSummary>> result = new HashMap<>();
        for (SkuOptionValue link : links) {
            OptionValue optionValue = optionValues.get(link.getOptionValueId());
            if (optionValue == null) {
                continue;
            }
            OptionGroup optionGroup = optionGroups.get(optionValue.getOptionGroupId());
            result.computeIfAbsent(link.getSkuId(), id -> new ArrayList<>()).add(new SkuOptionSummary(
                    optionValue.getOptionGroupId(),
                    optionGroup != null ? optionGroup.getName() : null,
                    optionValue.getOptionValueId(),
                    optionValue.getValue()));
        }
        return result;
    }
}
