package com.kb.wms.product.application.port.in;

import com.kb.wms.product.application.port.in.command.OptionValueRegisterCommand;
import com.kb.wms.product.domain.entity.OptionValue;

/**
 * 옵션 값 등록 유스케이스.
 * POST /api/v1/products/option-groups/{optionGroupId}/values
 */
public interface OptionValueUseCase {

    OptionValue registerOptionValue(OptionValueRegisterCommand command);
}
