package com.kb.wms.auth.application.port.in;

import java.util.List;

import com.kb.wms.auth.application.port.in.command.ChangePasswordCommand;
import com.kb.wms.auth.application.port.in.command.InitialHqAdminCommand;
import com.kb.wms.auth.application.port.in.command.UserSignupCommand;
import com.kb.wms.auth.application.port.in.command.UserUpdateCommand;
import com.kb.wms.auth.application.port.in.query.UserSearchCondition;
import com.kb.wms.auth.application.port.in.result.InitialHqAdminResult;
import com.kb.wms.auth.domain.entity.User;

/**
 * 가입·계정 관리 유스케이스.
 * POST /api/v1/auth/signup, GET /api/v1/auth/me, PATCH /api/v1/auth/me/password,
 * GET /api/v1/users, PATCH /api/v1/users/{userId}
 */
public interface UserUseCase {

    /** 가입 신청. PENDING으로 생성한다. 아이디·이메일 중복은 409, HQ_ADMIN 가입과 형식 위반은 400. */
    User signUp(UserSignupCommand command);

    /** 사용자 한 명 조회. 없으면 404 USER_NOT_FOUND. */
    User getUser(Long userId);

    List<User> getUsers(UserSearchCondition condition);

    /** 부분 수정. 상태가 바뀌면 StatusHistory(USER)를 같은 트랜잭션에서 기록한다. */
    User updateUser(UserUpdateCommand command);

    /**
     * 본인 비밀번호 변경. 현재 비밀번호가 다르면 400 CURRENT_PASSWORD_MISMATCH, 새 비밀번호가 형식 위반이거나
     * 현재와 같으면 400, 비활성 계정은 403, 사용자가 없으면 404. 이미 발급된 토큰은 그대로 유효하다(ADR-011).
     */
    void changePassword(ChangePasswordCommand command);

    /**
     * 본사 관리자가 하나도 없을 때만 설정값으로 ACTIVE 관리자를 만든다(시작 시 초기화용, ADR-013).
     * 이미 있으면 ALREADY_EXISTS, 설정이 모두 비어 있으면 NOT_CONFIGURED를 돌려준다.
     * 설정이 일부만 비었거나 아이디·비밀번호 형식 위반이면 400, 아이디·이메일 중복이면 409.
     */
    InitialHqAdminResult ensureInitialHqAdmin(InitialHqAdminCommand command);
}
