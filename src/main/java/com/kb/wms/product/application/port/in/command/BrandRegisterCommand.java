package com.kb.wms.product.application.port.in.command;

/**
 * POST /api/v1/products/brands 요청.
 *
 * @param brandCode 영문 3자. 서버가 대문자로 정규화해 저장한다.
 */
public record BrandRegisterCommand(
        String brandCode,
        String name,
        String description
) {
}
