package com.kb.wms.product.application.port.in;

import com.kb.wms.product.application.port.in.query.ProductSkuSearchCondition;
import java.util.List;

import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.product.application.port.in.command.ProductSkuRegisterCommand;
import com.kb.wms.product.application.port.in.command.ProductSkuUpdateCommand;
import com.kb.wms.product.application.port.in.command.SkuOptionConnectCommand;
import com.kb.wms.product.application.port.in.result.SkuOptionSummary;
import com.kb.wms.product.domain.entity.ProductSku;

/**
 * SKU 등록/조회/옵션 연결 유스케이스.
 * POST /api/v1/products/skus, GET /api/v1/products/skus, GET /api/v1/products/skus/{skuId},
 * POST /api/v1/products/skus/{skuId}/options
 */
public interface ProductSkuUseCase {

    ProductSku registerSku(ProductSkuRegisterCommand command);

    /**
     * 값이 있는 필드만 수정한다(비활성 SKU도 가능). 바코드가 다른 SKU와 겹치면 DUPLICATE_BARCODE,
     * 단가가 바뀌면 처리자와 함께 단가 변경 이력을 남긴다.
     */
    ProductSku updateSku(ProductSkuUpdateCommand command, AuthenticatedUser actor);

    /** 필터의 상품·브랜드·카테고리가 없으면 PRODUCT_NOT_FOUND·BRAND_NOT_FOUND·CATEGORY_NOT_FOUND */
    List<ProductSku> getSkus(ProductSkuSearchCondition condition);

    ProductSku getSku(Long skuId);

    /** 사용자 요청용 목록 조회. 점주(STORE_OWNER)에게는 활성 SKU만 돌려준다. */
    List<ProductSku> getSkus(ProductSkuSearchCondition condition, AuthenticatedUser actor);

    /** 사용자 요청용 단건 조회. 점주가 비활성 SKU를 조회하면 SKU_NOT_FOUND(404)다. */
    ProductSku getSku(Long skuId, AuthenticatedUser actor);

    /**
     * 활성화는 상품이 비활성이면 PRODUCT_INACTIVE로 거절한다. 비활성화는 활성 SKU에 재고가 남아 있거나
     * 진행 중인 업무가 있으면 SKU_IN_USE로 거절한다. 이미 같은 상태면 그대로 반환한다.
     */
    ProductSku changeSkuStatus(Long skuId, boolean active);

    void connectOptions(SkuOptionConnectCommand command);

    List<SkuOptionSummary> getSkuOptions(Long skuId);

    /** 여러 SKU의 옵션을 쿼리 3번(연결·옵션 값·옵션 그룹)으로 조회한다. 옵션이 없는 SKU는 빈 목록이다. */
    java.util.Map<Long, List<SkuOptionSummary>> getSkuOptionsBySkuIds(java.util.Collection<Long> skuIds);
}
