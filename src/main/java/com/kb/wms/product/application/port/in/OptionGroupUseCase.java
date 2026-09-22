package com.kb.wms.product.application.port.in;

import java.util.List;

import com.kb.wms.product.application.port.in.command.OptionGroupRegisterCommand;
import com.kb.wms.product.domain.entity.OptionGroup;

/**
 * 옵션 그룹 등록/조회 유스케이스.
 * POST /api/v1/products/option-groups, GET /api/v1/products/{productId}/option-groups
 */
public interface OptionGroupUseCase {

    OptionGroup registerOptionGroup(OptionGroupRegisterCommand command);

    List<OptionGroup> getOptionGroupsByProduct(Long productId);
}
