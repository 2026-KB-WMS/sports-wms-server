package com.kb.wms.auth.adapter.in.startup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.kb.wms.auth.application.port.in.UserUseCase;
import com.kb.wms.auth.application.port.in.command.InitialHqAdminCommand;
import com.kb.wms.auth.application.port.in.result.InitialHqAdminResult;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class InitialHqAdminRunnerTest {

    @Mock
    private UserUseCase userUseCase;

    private InitialHqAdminRunner runner() {
        return new InitialHqAdminRunner(userUseCase, "hq_admin01", "P@ssw0rd!", "홍길동", "admin@example.com",
                "010-0000-0000");
    }

    @Test
    @DisplayName("설정값을 그대로 유스케이스에 넘긴다")
    void passesConfigurationToUseCase() {
        when(userUseCase.ensureInitialHqAdmin(any())).thenReturn(InitialHqAdminResult.CREATED);

        runner().run(null);

        ArgumentCaptor<InitialHqAdminCommand> captor = ArgumentCaptor.forClass(InitialHqAdminCommand.class);
        verify(userUseCase).ensureInitialHqAdmin(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new InitialHqAdminCommand(
                "hq_admin01", "P@ssw0rd!", "홍길동", "admin@example.com", "010-0000-0000"));
    }

    @Test
    @DisplayName("결과가 무엇이든 기동은 막지 않는다")
    void neverFailsStartupOnResult() {
        for (InitialHqAdminResult result : InitialHqAdminResult.values()) {
            when(userUseCase.ensureInitialHqAdmin(any())).thenReturn(result);

            assertThatCode(() -> runner().run(null)).doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("설정이 잘못돼 검증 오류가 나도 기동은 막지 않는다")
    void swallowsBusinessException() {
        when(userUseCase.ensureInitialHqAdmin(any()))
                .thenThrow(new BusinessException(ErrorCode.VALIDATION_ERROR, "설정 오류"));

        assertThatCode(() -> runner().run(null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("다른 인스턴스가 먼저 만들어 유니크 제약에 걸려도 기동은 막지 않는다")
    void swallowsConcurrentCreation() {
        when(userUseCase.ensureInitialHqAdmin(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatCode(() -> runner().run(null)).doesNotThrowAnyException();
    }
}
