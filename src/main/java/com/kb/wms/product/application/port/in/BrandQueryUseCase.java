package com.kb.wms.product.application.port.in;

import com.kb.wms.product.application.port.in.query.BrandSearchCondition;
import java.util.List;

import com.kb.wms.product.application.port.in.command.BrandRegisterCommand;
import com.kb.wms.product.application.port.in.command.BrandUpdateCommand;
import com.kb.wms.product.domain.entity.Brand;

/**
 * 브랜드 등록/수정/조회 유스케이스. POST, GET /api/v1/products/brands, PATCH /api/v1/products/brands/{brandId}
 */
public interface BrandQueryUseCase {

    Brand registerBrand(BrandRegisterCommand command);

    /** 이름·설명을 부분 수정한다. 없으면 BRAND_NOT_FOUND, 다른 브랜드와 이름이 겹치면 DUPLICATE_BRAND_NAME. */
    Brand updateBrand(BrandUpdateCommand command);

    List<Brand> getBrands(BrandSearchCondition condition);

    Brand getBrand(Long brandId);

    /** 브랜드 ID별 이름. 목록 응답에서 항목마다 조회하지 않도록 한 번에 읽는다. 없는 ID는 맵에 없다. */
    java.util.Map<Long, String> getBrandNames(java.util.Collection<Long> brandIds);
}
