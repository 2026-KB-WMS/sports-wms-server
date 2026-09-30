package com.kb.wms.inbound.adapter.in.web.dto.request;

import java.util.Optional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.inbound.application.port.in.command.SupplierUpdateCommand;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * PATCH /api/v1/suppliers/{supplierId} 요청 바디. 모든 필드는 선택이며, 보낸 필드만 수정한다.
 *
 * <p>JSON에서 키가 없으면 필드가 null(변경 없음), 키가 있고 값이 null이면 {@code Optional.empty()}로 들어온다.
 * 이 구분으로 email·address는 null을 보내면 값을 비우고, 필수 항목(supplierName·managerName·contactNumber)에
 * null을 보내면 {@code @NotBlank}로 400을 응답한다.
 *
 * <p>supplierCode와 거래 상태(isActive)는 이 API로 수정할 수 없다. 검증 애노테이션 대신
 * toCommand()에서 명시적으로 거부해, 명세대로 400 VALIDATION_ERROR로 응답한다.
 */
public record SupplierUpdateRequest(
        Optional<@NotBlank(message = "공급처명은 비울 수 없습니다.")
                 @Size(max = 200, message = "공급처명은 최대 200자입니다.") String> supplierName,

        Optional<@NotBlank(message = "공급처 담당자명은 비울 수 없습니다.")
                 @Size(max = 100, message = "공급처 담당자명은 최대 100자입니다.") String> managerName,

        Optional<@NotBlank(message = "공급처 연락처는 비울 수 없습니다.")
                 @Size(max = 30, message = "공급처 연락처는 최대 30자입니다.") String> contactNumber,

        Optional<@Email(message = "이메일 형식이 올바르지 않습니다.")
                 @Size(max = 255, message = "이메일은 최대 255자입니다.") String> email,

        Optional<@Size(max = 500, message = "공급처 주소는 최대 500자입니다.") String> address,

        String supplierCode,

        Boolean isActive
) {

    public SupplierUpdateCommand toCommand() {
        if (supplierCode != null || isActive != null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "supplierCode, isActive는 이 API로 수정할 수 없습니다.");
        }
        return new SupplierUpdateCommand(
                valueOf(supplierName),
                valueOf(managerName),
                valueOf(contactNumber),
                valueOf(email),
                valueOf(address),
                isCleared(email),
                isCleared(address));
    }

    private static String valueOf(Optional<String> field) {
        return field == null ? null : field.orElse(null);
    }

    /** 키는 있는데 값이 null인 경우(명시적으로 비움). */
    private static boolean isCleared(Optional<String> field) {
        return field != null && field.isEmpty();
    }
}
