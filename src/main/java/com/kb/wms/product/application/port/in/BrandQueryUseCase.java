package com.kb.wms.product.application.port.in;

import java.util.List;

import com.kb.wms.product.application.port.in.command.BrandRegisterCommand;
import com.kb.wms.product.domain.entity.Brand;

/**
 * 브랜드 등록/조회 유스케이스. POST, GET /api/v1/products/brands
 */
public interface BrandQueryUseCase {

    Brand registerBrand(BrandRegisterCommand command);

    List<Brand> getBrands();

    Brand getBrand(Long brandId);
}
