package com.kb.wms.common.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:api_timing_options;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.properties.hibernate.session_factory.statement_inspector=com.kb.wms.common.logging.QueryCountInspector",
        "wms.api-log.show-queries=true",
        "wms.api-log.screen-separator=true"
})
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class ApiTimingFilterOptionsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("show-queries가 켜져 있으면 요약 줄 다음에 실행한 SQL을 번호와 함께 남긴다")
    void logsExecutedQueries(CapturedOutput output) throws Exception {
        // 없는 아이디로 로그인하면 사용자 조회 쿼리를 실행한 뒤 401이다.
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"loginId\":\"no-such-user\",\"password\":\"pw\"}"));

        assertThat(output.getOut()).containsPattern(
                "POST /api/v1/auth/login -> 401 \\(\\d+ms, queries=[1-9]\\d*\\)\\R  \\[1\\] select ");
    }

    @Test
    @DisplayName("X-Screen 값이 바뀔 때만 구분선을 남기고, 헤더가 없으면 남기지 않는다")
    void logsSeparatorWhenScreenChanges(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/api/v1/products/brands").header("X-Screen", "screen-a"));
        mockMvc.perform(get("/api/v1/products/brands").header("X-Screen", "screen-a"));
        mockMvc.perform(get("/api/v1/products/brands"));
        mockMvc.perform(get("/api/v1/products/brands").header("X-Screen", "screen-b"));

        String out = output.getOut();
        assertThat(out).containsPattern("========== screen-a ==========\\RGET /api/v1/products/brands -> 401");
        assertThat(out).containsPattern("========== screen-b ==========\\RGET /api/v1/products/brands -> 401");
        assertThat(out.split("========== screen-a ==========", -1)).hasSize(2);
    }
}
