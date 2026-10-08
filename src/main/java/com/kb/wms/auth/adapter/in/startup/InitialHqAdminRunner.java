package com.kb.wms.auth.adapter.in.startup;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.kb.wms.auth.application.port.in.UserUseCase;
import com.kb.wms.auth.application.port.in.command.InitialHqAdminCommand;
import com.kb.wms.common.exception.BusinessException;

import lombok.extern.slf4j.Slf4j;

/**
 * 앱 시작 시 본사 관리자가 없으면 설정(wms.admin.*, 환경변수 WMS_ADMIN_*)으로 첫 관리자를 만든다 (ADR-013).
 * 설정이 잘못돼도 기동은 막지 않고 오류 로그만 남긴다. 비밀번호는 로그에 남기지 않는다.
 */
@Slf4j
@Component
public class InitialHqAdminRunner implements ApplicationRunner {

    private final UserUseCase userUseCase;
    private final InitialHqAdminCommand command;

    public InitialHqAdminRunner(UserUseCase userUseCase,
                                @Value("${wms.admin.login-id:}") String loginId,
                                @Value("${wms.admin.password:}") String password,
                                @Value("${wms.admin.name:}") String name,
                                @Value("${wms.admin.email:}") String email,
                                @Value("${wms.admin.phone:}") String phone) {
        this.userUseCase = userUseCase;
        this.command = new InitialHqAdminCommand(loginId, password, name, email, phone);
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            switch (userUseCase.ensureInitialHqAdmin(command)) {
                case CREATED -> log.info("최초 본사 관리자를 생성했습니다. loginId={}", command.loginId());
                case ALREADY_EXISTS -> log.debug("본사 관리자가 이미 있어 최초 관리자를 만들지 않습니다.");
                case NOT_CONFIGURED -> log.warn("본사 관리자가 없고 WMS_ADMIN_* 설정도 없습니다. "
                        + "가입 승인과 계정 관리를 하려면 설정하거나 DB에 직접 본사 관리자를 만들어야 합니다.");
            }
        } catch (BusinessException e) {
            log.error("최초 본사 관리자를 만들지 못했습니다: {}", e.getMessage());
        } catch (DataIntegrityViolationException e) {
            // 서버가 동시에 여러 대 뜰 때 다른 쪽이 먼저 만든 경우(유니크 제약)
            log.info("다른 인스턴스가 최초 본사 관리자를 먼저 만들었습니다.");
        }
    }
}
