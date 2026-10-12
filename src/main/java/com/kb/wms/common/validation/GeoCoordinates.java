package com.kb.wms.common.validation;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 주소와 함께 저장하는 위도·경도 값 규칙. 컬럼은 DECIMAL(10,7)이라 소수 7자리(약 1cm)로 맞춘다.
 */
public final class GeoCoordinates {

    public static final int SCALE = 7;

    private GeoCoordinates() {
    }

    /** 위도와 경도는 쌍으로만 입력할 수 있다. 둘 다 없거나 둘 다 있어야 한다. */
    public static boolean isPair(BigDecimal latitude, BigDecimal longitude) {
        return (latitude == null) == (longitude == null);
    }

    /** 지도 API가 소수 7자리를 넘겨 줄 수 있으므로 거절하지 않고 반올림한다. */
    public static BigDecimal scale(BigDecimal value) {
        return value == null ? null : value.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
