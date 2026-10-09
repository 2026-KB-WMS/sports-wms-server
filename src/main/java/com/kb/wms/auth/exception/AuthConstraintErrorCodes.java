package com.kb.wms.auth.exception;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.kb.wms.common.exception.ConstraintErrorCodeProvider;
import com.kb.wms.common.exception.DomainErrorCode;

/**
 * 회원 도메인 유니크 제약 조건 → 오류 코드 (V14__create_users_table.sql 기준).
 */
@Component
public class AuthConstraintErrorCodes implements ConstraintErrorCodeProvider {

    @Override
    public Map<String, DomainErrorCode> constraintErrorCodes() {
        return Map.of(
                "uk_users_login_id", AuthErrorCode.DUPLICATE_LOGIN_ID,
                "uk_users_email", AuthErrorCode.DUPLICATE_EMAIL);
    }
}
