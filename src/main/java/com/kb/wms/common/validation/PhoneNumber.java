package com.kb.wms.common.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;

/**
 * 전화번호 형식: 하이픈 포함, 지역번호 2~3자리 - 국번 3~4자리 - 4자리 (예: 02-123-4567, 031-1234-5678, 010-1234-5678).
 * null은 통과하므로 필수 여부는 {@code @NotBlank}가 따로 결정한다.
 */
@Documented
@Constraint(validatedBy = {})
@Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$")
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PhoneNumber {

    String message() default "전화번호 형식이 올바르지 않습니다. 예: 010-1234-5678, 02-123-4567";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
