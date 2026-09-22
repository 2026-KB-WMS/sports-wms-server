package com.kb.wms.product.exception;

import com.kb.wms.common.exception.DomainErrorCode;
import com.kb.wms.common.exception.ErrorCode;

import lombok.Getter;

/**
 * 상품 도메인 특수 오류 코드.
 * 공통 API 규칙 문서의 기본 오류 코드 세트(ErrorCode)는 그대로 두고,
 * error_code 응답 필드만 여기 정의된 값으로 세분화한다.
 */
@Getter
public enum ProductErrorCode implements DomainErrorCode {

    BRAND_NOT_FOUND(ErrorCode.NOT_FOUND, "브랜드를 찾을 수 없습니다."),
    CATEGORY_NOT_FOUND(ErrorCode.NOT_FOUND, "카테고리를 찾을 수 없습니다."),
    PARENT_CATEGORY_NOT_FOUND(ErrorCode.NOT_FOUND, "상위 카테고리를 찾을 수 없습니다."),
    CATEGORY_CODE_DUPLICATED(ErrorCode.CONFLICT, "이미 존재하는 카테고리 코드입니다."),
    PRODUCT_NOT_FOUND(ErrorCode.NOT_FOUND, "상품을 찾을 수 없습니다."),
    PRODUCT_CODE_DUPLICATED(ErrorCode.CONFLICT, "이미 존재하는 상품 코드입니다."),
    OPTION_GROUP_NOT_FOUND(ErrorCode.NOT_FOUND, "옵션 그룹을 찾을 수 없습니다."),
    OPTION_GROUP_NAME_DUPLICATED(ErrorCode.CONFLICT, "이미 존재하는 옵션 그룹명입니다."),
    OPTION_VALUE_NOT_FOUND(ErrorCode.NOT_FOUND, "옵션 값을 찾을 수 없습니다."),
    OPTION_VALUE_DUPLICATED(ErrorCode.CONFLICT, "이미 존재하는 옵션 값입니다."),
    SKU_NOT_FOUND(ErrorCode.NOT_FOUND, "SKU를 찾을 수 없습니다."),
    SKU_CODE_DUPLICATED(ErrorCode.CONFLICT, "이미 존재하는 SKU 코드입니다."),
    BARCODE_DUPLICATED(ErrorCode.CONFLICT, "이미 존재하는 바코드입니다."),
    DUPLICATE_OPTION_VALUE(ErrorCode.CONFLICT, "이미 연결된 옵션 값입니다.");

    private final ErrorCode errorCode;
    private final String defaultMessage;

    ProductErrorCode(ErrorCode errorCode, String defaultMessage) {
        this.errorCode = errorCode;
        this.defaultMessage = defaultMessage;
    }
}
