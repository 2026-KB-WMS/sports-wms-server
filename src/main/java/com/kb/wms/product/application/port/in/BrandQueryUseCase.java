package com.kb.wms.product.application.port.in;

import java.util.List;

import com.kb.wms.product.domain.entity.Brand;

/**
 * 브랜드 조회 유스케이스. GET /api/v1/products/brands
 */
public interface BrandQueryUseCase {

    List<Brand> getBrands();
}
