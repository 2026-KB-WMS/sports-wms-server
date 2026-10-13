package com.kb.wms.common.locationcode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;

class RegionCodeTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "서울 강남구 테헤란로 1|SU",
            "서울특별시 강남구 테헤란로 1|SU",
            "  부산광역시 해운대구|BS",
            "경기 성남시 분당구|GG",
            "경기도 성남시 분당구|GG",
            "강원특별자치도 춘천시|GW",
            "전북특별자치도 전주시|JB",
            "전라북도 전주시|JB",
            "충남 천안시|CN",
            "경상남도 창원시|GN",
            "세종특별자치시 한누리대로|SJ",
            "제주특별자치도 제주시|JJ"})
    @DisplayName("주소의 첫 단어로 시·도 코드를 판별한다")
    void findByAddress(String address, RegionCode expected) {
        assertThat(RegionCode.findByAddress(address)).contains(expected);
    }

    @Test
    @DisplayName("17개 시·도 코드는 서로 겹치지 않고 모두 영문 2자다")
    void codesAreUniqueTwoLetters() {
        assertThat(RegionCode.values()).hasSize(17);
        assertThat(RegionCode.values()).allSatisfy(region -> assertThat(region.name()).matches("[A-Z]{2}"));
    }

    @Test
    @DisplayName("첫 단어가 시·도가 아니면 판별하지 못하고 VALIDATION_ERROR를 던진다")
    void unknownRegion() {
        assertThat(RegionCode.findByAddress("강남구 테헤란로 1")).isEmpty();
        assertThat(RegionCode.findByAddress("서울강남구")).isEmpty();
        assertThat(RegionCode.findByAddress(null)).isEmpty();
        assertThatThrownBy(() -> RegionCode.fromAddress("강남구 테헤란로 1"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.VALIDATION_ERROR.name());
    }

    @Test
    @DisplayName("발급 형식의 코드에서만 시·도를 읽는다")
    void regionOfIssuedCode() {
        assertThat(LocationCodeIssuer.regionOf("WH-SU-001")).contains(RegionCode.SU);
        assertThat(LocationCodeIssuer.regionOf("ST-GG-1000")).contains(RegionCode.GG);
        assertThat(LocationCodeIssuer.regionOf("WH-001")).isEmpty();
        assertThat(LocationCodeIssuer.regionOf("WH-XX-001")).isEmpty();
        assertThat(LocationCodeIssuer.regionOf("ST-GANGNAM")).isEmpty();
        assertThat(LocationCodeIssuer.regionOf(null)).isEmpty();
    }
}
