package com.kb.wms.common.security;

import java.util.Optional;

/**
 * 토큰의 사용자 ID로 현재 인증 주체(역할·소속)를 읽어 오는 포트. 인증 필터가 요청마다 호출하며,
 * 구현은 auth 도메인이 맡는다. 토큰 값이 아니라 DB 기준이라 소속·역할·상태 변경이 다음 요청부터 반영된다(ADR-016).
 */
public interface AuthenticatedUserResolver {

    /** 사용자가 없거나 ACTIVE가 아니면 비어 있다. */
    Optional<AuthenticatedUser> resolve(Long userId);
}
