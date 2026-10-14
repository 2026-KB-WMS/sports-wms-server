package com.kb.wms.warehouse.adapter.out.auth;

import org.springframework.stereotype.Component;

import com.kb.wms.auth.application.port.in.UserUseCase;
import com.kb.wms.auth.application.port.in.result.UserView;
import com.kb.wms.warehouse.application.port.out.WarehouseMemberUserPort;

import lombok.RequiredArgsConstructor;

/**
 * WarehouseMemberUserPort 포트를 인증 도메인의 유스케이스에 연결한다. 인증 도메인의 결과 객체를 이 도메인 자기 타입으로 바꾸는 유일한 곳이다.
 */
@Component
@RequiredArgsConstructor
public class WarehouseMemberUserAdapter implements WarehouseMemberUserPort {

    private final UserUseCase userUseCase;

    @Override
    public Candidate getCandidate(Long userId) {
        UserView user = userUseCase.getUserView(userId);
        return new Candidate(user.name(), user.loginId(), toRole(user.role()), user.active());
    }

    private static MemberRole toRole(String role) {
        return switch (role) {
            case "WAREHOUSE_MANAGER" -> MemberRole.WAREHOUSE_MANAGER;
            default -> MemberRole.OTHER;
        };
    }
}
