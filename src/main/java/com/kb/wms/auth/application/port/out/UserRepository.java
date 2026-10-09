package com.kb.wms.auth.application.port.out;

import java.util.List;
import java.util.Optional;

import com.kb.wms.auth.application.port.in.query.UserSearchCondition;
import com.kb.wms.auth.domain.entity.User;
import com.kb.wms.auth.domain.enums.UserRole;

/**
 * 회원 영속성 아웃바운드 포트.
 */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(Long userId);

    /** 상태 변경처럼 동시 수정을 막아야 할 때 행을 잠그고 조회한다. */
    Optional<User> findByIdForUpdate(Long userId);

    /** 로그인 아이디로 조회한다. 아이디는 소문자로 통일해 저장하므로 호출 전에 같은 형식으로 맞춘다. */
    Optional<User> findByLoginId(String loginId);

    /** 조건이 null이면 해당 조건은 무시한다. 생성 일시 내림차순. */
    List<User> search(UserSearchCondition condition);

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);

    /** 본인을 제외하고 같은 이메일을 쓰는 계정이 있는가 (사용자 수정 시 중복 검사). */
    boolean existsByEmailAndUserIdNot(String email, Long userId);

    /** 해당 역할의 계정이 하나라도 있는가 (최초 본사 관리자 생성 여부 판단). */
    boolean existsByRole(UserRole role);
}
