package com.kb.wms.product.exception;

import com.kb.wms.common.exception.DomainErrorCode;
import com.kb.wms.common.exception.ErrorCode;

import lombok.Getter;

/**
 * 상품 도메인 특수 오류 코드.
 * 클라이언트가 오류 원인을 세분화해서 처리할 수 있도록, 404(NOT_FOUND)·비활성 409(CONFLICT)를 포함한
 * 모든 상품 도메인 오류에 공통 코드 대신 이 도메인 전용 error_code 값을 사용한다.
 * WMS API 명세(Notion)의 각 엔드포인트 오류 표도 이 값들로 갱신한다.
 */
@Getter
public enum ProductErrorCode implements DomainErrorCode {

    BRAND_NOT_FOUND(ErrorCode.NOT_FOUND, "브랜드를 찾을 수 없습니다."),
    BRAND_INACTIVE(ErrorCode.CONFLICT, "비활성 브랜드입니다."),
    DUPLICATE_BRAND_NAME(ErrorCode.CONFLICT, "이미 등록된 브랜드명입니다."),
    CATEGORY_NOT_FOUND(ErrorCode.NOT_FOUND, "카테고리를 찾을 수 없습니다."),
    CATEGORY_INACTIVE(ErrorCode.CONFLICT, "비활성 카테고리입니다."),
    PARENT_CATEGORY_NOT_FOUND(ErrorCode.NOT_FOUND, "상위 카테고리를 찾을 수 없습니다."),
    PARENT_CATEGORY_INACTIVE(ErrorCode.CONFLICT, "비활성 상위 카테고리에는 하위 카테고리를 등록할 수 없습니다."),
    DUPLICATE_CATEGORY_CODE(ErrorCode.CONFLICT, "이미 존재하는 카테고리 코드입니다."),
    PRODUCT_NOT_FOUND(ErrorCode.NOT_FOUND, "상품을 찾을 수 없습니다."),
    PRODUCT_INACTIVE(ErrorCode.CONFLICT, "비활성 상품입니다."),
    DUPLICATE_PRODUCT_CODE(ErrorCode.CONFLICT, "이미 존재하는 상품 코드입니다."),
    OPTION_GROUP_NOT_FOUND(ErrorCode.NOT_FOUND, "옵션 그룹을 찾을 수 없습니다."),
    DUPLICATE_OPTION_GROUP_NAME(ErrorCode.CONFLICT, "이미 존재하는 옵션 그룹명입니다."),
    OPTION_VALUE_NOT_FOUND(ErrorCode.NOT_FOUND, "옵션 값을 찾을 수 없습니다."),
    OPTION_VALUE_INACTIVE(ErrorCode.CONFLICT, "비활성 옵션 값은 연결할 수 없습니다."),
    DUPLICATE_OPTION_VALUE(ErrorCode.CONFLICT, "이미 존재하거나 이미 연결된 옵션 값입니다."),
    OPTION_GROUP_CONFLICT(ErrorCode.CONFLICT, "같은 옵션 그룹에서는 하나의 옵션 값만 연결할 수 있습니다."),
    SKU_NOT_FOUND(ErrorCode.NOT_FOUND, "SKU를 찾을 수 없습니다."),
    SKU_INACTIVE(ErrorCode.CONFLICT, "비활성 SKU입니다."),
    DUPLICATE_SKU_CODE(ErrorCode.CONFLICT, "이미 존재하는 SKU 코드입니다."),
    DUPLICATE_BARCODE(ErrorCode.CONFLICT, "이미 존재하는 바코드입니다.");

    private final ErrorCode errorCode;
    private final String defaultMessage;

    ProductErrorCode(ErrorCode errorCode, String defaultMessage) {
        this.errorCode = errorCode;
        this.defaultMessage = defaultMessage;
    }
}
