package com.kb.wms.product.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.product.application.port.in.ProductSkuUseCase;
import com.kb.wms.product.application.port.in.command.ProductSkuRegisterCommand;
import com.kb.wms.product.application.port.in.command.SkuOptionConnectCommand;
import com.kb.wms.product.application.port.out.OptionValueRepository;
import com.kb.wms.product.application.port.out.ProductRepository;
import com.kb.wms.product.application.port.out.ProductSkuRepository;
import com.kb.wms.product.application.port.out.SkuOptionValueRepository;
import com.kb.wms.product.domain.entity.ProductSku;
import com.kb.wms.product.domain.entity.SkuOptionValue;

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

    private static final String DUPLICATE_OPTION_VALUE = "DUPLICATE_OPTION_VALUE";

    private final ProductSkuRepository productSkuRepository;
    private final ProductRepository productRepository;
    private final OptionValueRepository optionValueRepository;
    private final SkuOptionValueRepository skuOptionValueRepository;

    @Override
    @Transactional
    public ProductSku registerSku(ProductSkuRegisterCommand command) {
        if (!productRepository.findById(command.productId()).isPresent()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "상품을 찾을 수 없습니다.");
        }
        if (productSkuRepository.existsBySkuCode(command.skuCode())) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 존재하는 SKU 코드입니다.");
        }
        if (command.barcode() != null && productSkuRepository.existsByBarcode(command.barcode())) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 존재하는 바코드입니다.");
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
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "SKU를 찾을 수 없습니다."));
    }

    @Override
    @Transactional
    public void connectOptions(SkuOptionConnectCommand command) {
        ProductSku sku = productSkuRepository.findById(command.skuId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "SKU를 찾을 수 없습니다."));

        for (Long optionValueId : command.optionValueIds()) {
            if (!optionValueRepository.findById(optionValueId).isPresent()) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "옵션 값을 찾을 수 없습니다.");
            }
            if (skuOptionValueRepository.existsBySkuIdAndOptionValueId(sku.getSkuId(), optionValueId)) {
                throw new BusinessException(
                        ErrorCode.CONFLICT, "이미 연결된 옵션 값입니다.", DUPLICATE_OPTION_VALUE);
            }
            skuOptionValueRepository.save(SkuOptionValue.connect(sku.getSkuId(), optionValueId));
        }
    }
}
