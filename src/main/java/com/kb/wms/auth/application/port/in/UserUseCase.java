package com.kb.wms.auth.application.port.in;

import java.util.List;

import com.kb.wms.auth.application.port.in.command.UserSignupCommand;
import com.kb.wms.auth.application.port.in.command.UserUpdateCommand;
import com.kb.wms.auth.application.port.in.query.UserSearchCondition;
import com.kb.wms.auth.domain.entity.User;

/**
 * 가입·계정 관리 유스케이스.
 * POST /api/v1/auth/signup, GET /api/v1/users, PATCH /api/v1/users/{userId}
 */
public interface UserUseCase {

    /** 가입 신청. PENDING으로 생성한다. 아이디·이메일 중복은 409, HQ_ADMIN 가입과 형식 위반은 400. */
    User signUp(UserSignupCommand command);

    List<User> getUsers(UserSearchCondition condition);

    /** 부분 수정. 상태가 바뀌면 StatusHistory(USER)를 같은 트랜잭션에서 기록한다. */
    User updateUser(UserUpdateCommand command);
}
