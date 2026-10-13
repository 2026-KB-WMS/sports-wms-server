package com.kb.wms.product.application.port.in;

import java.util.List;

import com.kb.wms.product.application.port.in.command.OptionGroupRegisterCommand;
import com.kb.wms.product.application.port.in.result.ProductOptionGroupSummary;
import com.kb.wms.product.domain.entity.OptionGroup;

/**
 * 옵션 그룹 등록/조회 유스케이스.
 * POST, GET /api/v1/products/option-groups, GET /api/v1/products/{productId}/option-groups
 */
public interface OptionGroupUseCase {

    OptionGroup registerOptionGroup(OptionGroupRegisterCommand command);

    List<ProductOptionGroupSummary> getOptionGroupsByProduct(Long productId);

    /** 모든 옵션 그룹과 옵션 값(비활성 포함). 옵션 그룹 ID 오름차순, 값은 노출 순서 오름차순. */
    List<ProductOptionGroupSummary> getOptionGroups();
}
