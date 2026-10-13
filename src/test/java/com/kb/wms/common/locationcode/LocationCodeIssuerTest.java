package com.kb.wms.common.locationcode;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * 같은 H2 DB를 쓰는 다른 통합 테스트가 코드를 이미 커밋했을 수 있으므로, 절대 번호가 아니라
 * 첫 발급 번호를 기준으로 한 상대 값을 검증한다.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "spring.flyway.enabled=false")
class LocationCodeIssuerTest {

    @Autowired
    LocationCodeIssuer issuer;

    private static long number(String code) {
        return Long.parseLong(code.substring(code.lastIndexOf('-') + 1));
    }

    private static String code(String prefix, long number) {
        return String.format("%s-%03d", prefix, number);
    }

    @Test
    @DisplayName("종류+시·도별로 3자리 일련번호를 1씩 올려 발급하고, 종류·시·도가 다르면 번호를 따로 센다")
    void issuesSequentiallyPerTypeAndRegion() {
        String first = issuer.issue(LocationType.WAREHOUSE, RegionCode.SU, c -> false);
        String second = issuer.issue(LocationType.WAREHOUSE, RegionCode.SU, c -> false);

        assertThat(first).matches("WH-SU-\\d{3,}");
        assertThat(number(second)).isEqualTo(number(first) + 1);

        String otherRegion = issuer.issue(LocationType.WAREHOUSE, RegionCode.BS, c -> false);
        String otherType = issuer.issue(LocationType.STORE, RegionCode.SU, c -> false);
        assertThat(otherRegion).startsWith("WH-BS-");
        assertThat(otherType).startsWith("ST-SU-");
        assertThat(issuer.issue(LocationType.WAREHOUSE, RegionCode.BS, c -> false))
                .isEqualTo(code("WH-BS", number(otherRegion) + 1));
        assertThat(issuer.issue(LocationType.STORE, RegionCode.SU, c -> false))
                .isEqualTo(code("ST-SU", number(otherType) + 1));
    }

    @Test
    @DisplayName("이미 있는 코드와 겹치면 다음 번호로 넘어간다")
    void skipsExistingCodes() {
        long next = number(issuer.issue(LocationType.STORE, RegionCode.GG, c -> false)) + 1;
        Set<String> existing = Set.of(code("ST-GG", next), code("ST-GG", next + 1));

        assertThat(issuer.issue(LocationType.STORE, RegionCode.GG, existing::contains))
                .isEqualTo(code("ST-GG", next + 2));
    }

    @Test
    @DisplayName("발급한 번호는 그 코드가 어디에도 남아 있지 않아도 다시 쓰지 않는다")
    void doesNotReuseNumbers() {
        String first = issuer.issue(LocationType.STORE, RegionCode.JJ, c -> false);
        // first 코드를 쓰던 지점이 다른 시·도로 옮겨 가 이 코드가 어디에도 없어도 다음 발급은 +1이다.
        String second = issuer.issue(LocationType.STORE, RegionCode.JJ, c -> false);

        assertThat(number(second)).isEqualTo(number(first) + 1);
    }
}
