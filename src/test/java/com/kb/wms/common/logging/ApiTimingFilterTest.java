package com.kb.wms.common.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:api_timing;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.properties.hibernate.session_factory.statement_inspector=com.kb.wms.common.logging.QueryCountInspector"
})
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class ApiTimingFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("요청마다 메서드·경로·상태·실행시간·쿼리 횟수를 한 줄로 남긴다")
    void logsMethodPathStatusTimeAndQueryCount(CapturedOutput output) throws Exception {
        // 토큰이 없으면 401이고 DB에는 닿지 않는다.
        mockMvc.perform(get("/api/v1/products/brands"));

        assertThat(output.getOut()).containsPattern("GET /api/v1/products/brands -> 401 \\(\\d+ms, queries=0\\)");
    }
}
