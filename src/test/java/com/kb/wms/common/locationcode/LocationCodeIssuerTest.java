package com.kb.wms.common.locationcode;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@TestPropertySource(properties = "spring.flyway.enabled=false")
class LocationCodeIssuerTest {

    @Autowired
    LocationCodeIssuer issuer;

    @Test
    @DisplayName("종류+시·도별로 001부터 순서대로 발급한다")
    void issuesSequentiallyPerTypeAndRegion() {
        assertThat(issuer.issue(LocationType.WAREHOUSE, RegionCode.SU, c -> false)).isEqualTo("WH-SU-001");
        assertThat(issuer.issue(LocationType.WAREHOUSE, RegionCode.SU, c -> false)).isEqualTo("WH-SU-002");
        assertThat(issuer.issue(LocationType.WAREHOUSE, RegionCode.BS, c -> false)).isEqualTo("WH-BS-001");
        assertThat(issuer.issue(LocationType.STORE, RegionCode.SU, c -> false)).isEqualTo("ST-SU-001");
    }

    @Test
    @DisplayName("이미 있는 코드와 겹치면 다음 번호로 넘어간다")
    void skipsExistingCodes() {
        Set<String> existing = Set.of("ST-GG-001", "ST-GG-002");

        assertThat(issuer.issue(LocationType.STORE, RegionCode.GG, existing::contains)).isEqualTo("ST-GG-003");
    }

    @Test
    @DisplayName("발급한 번호는 코드가 사라져도 다시 쓰지 않는다")
    void doesNotReuseNumbers() {
        assertThat(issuer.issue(LocationType.STORE, RegionCode.JJ, c -> false)).isEqualTo("ST-JJ-001");
        // 001을 쓰던 지점이 다른 시·도로 옮겨 가 해당 코드가 어디에도 없어도 다음 발급은 002다.
        assertThat(issuer.issue(LocationType.STORE, RegionCode.JJ, c -> false)).isEqualTo("ST-JJ-002");
    }
}
