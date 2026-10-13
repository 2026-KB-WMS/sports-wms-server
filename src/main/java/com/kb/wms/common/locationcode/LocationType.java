package com.kb.wms.common.locationcode;

import lombok.Getter;

/**
 * 코드를 자동 발급하는 거점 종류와 코드 접두어.
 */
@Getter
public enum LocationType {

    WAREHOUSE("WH"),
    STORE("ST");

    private final String prefix;

    LocationType(String prefix) {
        this.prefix = prefix;
    }
}
