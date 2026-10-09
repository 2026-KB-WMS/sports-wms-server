package com.kb.wms.auth.application.port.in;

import com.kb.wms.auth.application.port.in.command.LoginCommand;
import com.kb.wms.auth.application.port.in.result.LoginResult;

/**
 * 로그인 유스케이스. POST /api/v1/auth/login
 */
public interface AuthUseCase {

    /**
     * 아이디·비밀번호가 맞고 ACTIVE 계정이면 액세스 토큰을 발급하고 last_login_at을 갱신한다.
     * 없는 아이디와 비밀번호 불일치는 같은 401, 비밀번호 검증 뒤에 PENDING·INACTIVE는 403이다.
     */
    LoginResult login(LoginCommand command);
}
