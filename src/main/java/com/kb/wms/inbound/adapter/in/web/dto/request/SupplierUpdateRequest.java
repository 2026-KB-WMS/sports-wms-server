package com.kb.wms.inbound.adapter.in.web.dto.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.inbound.application.port.in.command.SupplierUpdateCommand;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * PATCH /api/v1/suppliers/{supplierId} 요청 바디. 모든 필드는 선택이며, 보낸 필드만 수정한다.
 *
 * <p>email·address는 null을 보내면 값을 비우므로 "키 없음(변경 없음)"과 "키는 있는데 null(비움)"을 구분해야 한다.
 * Jackson은 키가 없을 때도 Optional 필드에 {@code Optional.empty()}를 넣어 두 경우를 구분하지 못하므로,
 * 세터가 호출됐는지(= JSON에 키가 있었는지)를 별도 플래그로 기록한다.
 *
 * <p>supplierName·managerName·contactNumber는 비울 수 없는 필수 항목이라, 키가 있는데 null이거나 빈 문자열이면
 * 400 VALIDATION_ERROR로 거절한다. supplierCode와 거래 상태(isActive)는 이 API로 수정할 수 없으므로
 * toCommand()에서 명시적으로 거부한다.
 */
public class SupplierUpdateRequest {

    @Size(max = 200, message = "공급처명은 최대 200자입니다.")
    private String supplierName;
    private boolean supplierNamePresent;

    @Size(max = 100, message = "공급처 담당자명은 최대 100자입니다.")
    private String managerName;
    private boolean managerNamePresent;

    @Size(max = 30, message = "공급처 연락처는 최대 30자입니다.")
    private String contactNumber;
    private boolean contactNumberPresent;

    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Size(max = 255, message = "이메일은 최대 255자입니다.")
    private String email;
    private boolean emailPresent;

    @Size(max = 500, message = "공급처 주소는 최대 500자입니다.")
    private String address;
    private boolean addressPresent;

    private String supplierCode;
    private Boolean isActive;

    @JsonSetter("supplierName")
    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
        this.supplierNamePresent = true;
    }

    @JsonSetter("managerName")
    public void setManagerName(String managerName) {
        this.managerName = managerName;
        this.managerNamePresent = true;
    }

    @JsonSetter("contactNumber")
    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
        this.contactNumberPresent = true;
    }

    @JsonSetter("email")
    public void setEmail(String email) {
        this.email = email;
        this.emailPresent = true;
    }

    @JsonSetter("address")
    public void setAddress(String address) {
        this.address = address;
        this.addressPresent = true;
    }

    @JsonSetter("supplierCode")
    public void setSupplierCode(String supplierCode) {
        this.supplierCode = supplierCode;
    }

    @JsonSetter("isActive")
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public SupplierUpdateCommand toCommand() {
        if (supplierCode != null || isActive != null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "supplierCode, isActive는 이 API로 수정할 수 없습니다.");
        }
        rejectIfBlank(supplierNamePresent, supplierName, "공급처명은 비울 수 없습니다.");
        rejectIfBlank(managerNamePresent, managerName, "공급처 담당자명은 비울 수 없습니다.");
        rejectIfBlank(contactNumberPresent, contactNumber, "공급처 연락처는 비울 수 없습니다.");

        return new SupplierUpdateCommand(
                supplierName,
                managerName,
                contactNumber,
                email,
                address,
                emailPresent && email == null,
                addressPresent && address == null);
    }

    /** 키가 있는데 null이거나 공백뿐이면 필수 항목을 비우려는 요청이므로 거절한다. */
    private static void rejectIfBlank(boolean present, String value, String message) {
        if (present && (value == null || value.isBlank())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, message);
        }
    }
}
