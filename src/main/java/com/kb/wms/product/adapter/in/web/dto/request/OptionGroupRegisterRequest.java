package com.kb.wms.product.adapter.in.web.dto.request;

import com.kb.wms.product.application.port.in.command.OptionGroupRegisterCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * POST /api/v1/products/option-groups 요청 바디.
 */
public record OptionGroupRegisterRequest(
        @NotBlank(message = "옵션 그룹명은 필수 값입니다.")
        @Size(max = 100, message = "옵션 그룹명은 최대 100자입니다.")
        String name
) {

    public OptionGroupRegisterCommand toCommand() {
        return new OptionGroupRegisterCommand(name);
    }
}
