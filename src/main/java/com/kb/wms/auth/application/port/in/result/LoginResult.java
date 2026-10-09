package com.kb.wms.auth.application.port.in.result;

import com.kb.wms.auth.domain.entity.User;

/**
 * 로그인 성공 결과.
 *
 * @param expiresIn 액세스 토큰 유효 시간(초)
 */
public record LoginResult(
        String accessToken,
        long expiresIn,
        User user
) {
}
