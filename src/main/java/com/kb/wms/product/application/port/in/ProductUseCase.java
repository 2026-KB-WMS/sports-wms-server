package com.kb.wms.product.application.port.in;

import com.kb.wms.common.paging.PageCondition;
import com.kb.wms.common.paging.PageResult;
import com.kb.wms.product.application.port.in.query.ProductSearchCondition;
import java.util.List;

import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.product.application.port.in.command.ProductRegisterCommand;
import com.kb.wms.product.application.port.in.command.ProductUpdateCommand;
import com.kb.wms.product.application.port.in.result.ProductSpecSummary;
import com.kb.wms.product.domain.entity.Product;

/**
 * 상품 등록/조회 유스케이스.
 * POST /api/v1/products, GET /api/v1/products, GET /api/v1/products/{productId},
 * PATCH /api/v1/products/{productId}
 */
public interface ProductUseCase {

    Product registerProduct(ProductRegisterCommand command);

    Product updateProduct(ProductUpdateCommand command);

    /** 필터의 브랜드·카테고리가 없으면 BRAND_NOT_FOUND·CATEGORY_NOT_FOUND */
    PageResult<Product> getProducts(ProductSearchCondition condition, PageCondition page);

    Product getProduct(Long productId);

    /** 상품 ID별 이름. SKU 목록 응답에서 항목마다 조회하지 않도록 한 번에 읽는다. 없는 ID는 맵에 없다. */
    java.util.Map<Long, String> getProductNames(java.util.Collection<Long> productIds);

    /** 사용자 요청용 목록 조회. 점주(STORE_OWNER)에게는 활성 상품만 돌려준다. */
    PageResult<Product> getProducts(ProductSearchCondition condition, PageCondition page, AuthenticatedUser actor);

    /** 사용자 요청용 단건 조회. 점주가 비활성 상품을 조회하면 PRODUCT_NOT_FOUND(404)다. */
    Product getProduct(Long productId, AuthenticatedUser actor);

    /** 상품에 지정된 스펙 값(옵션 그룹 ID 오름차순). 없으면 빈 목록. */
    List<ProductSpecSummary> getProductSpecs(Long productId);
}
