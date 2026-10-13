package com.kb.wms.common.locationcode;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;

import lombok.Getter;

/**
 * 창고·지점 코드에 들어가는 시·도 코드 (영문 2자). 주소의 첫 단어(정식명·약칭)로 판별한다.
 */
@Getter
public enum RegionCode {

    SU("서울", "서울", "서울시", "서울특별시"),
    BS("부산", "부산", "부산시", "부산광역시"),
    DG("대구", "대구", "대구시", "대구광역시"),
    IC("인천", "인천", "인천시", "인천광역시"),
    GJ("광주", "광주", "광주시", "광주광역시"),
    DJ("대전", "대전", "대전시", "대전광역시"),
    US("울산", "울산", "울산시", "울산광역시"),
    SJ("세종", "세종", "세종시", "세종특별자치시"),
    GG("경기", "경기", "경기도"),
    GW("강원", "강원", "강원도", "강원특별자치도"),
    CB("충북", "충북", "충청북도"),
    CN("충남", "충남", "충청남도"),
    JB("전북", "전북", "전라북도", "전북특별자치도"),
    JN("전남", "전남", "전라남도"),
    GB("경북", "경북", "경상북도"),
    GN("경남", "경남", "경상남도"),
    JJ("제주", "제주", "제주도", "제주특별자치도");

    private final String displayName;
    private final List<String> aliases;

    RegionCode(String displayName, String... aliases) {
        this.displayName = displayName;
        this.aliases = List.of(aliases);
    }

    /** 주소 첫 단어가 시·도 정식명 또는 약칭과 일치하는 지역. 없으면 비어 있다. */
    public static Optional<RegionCode> findByAddress(String address) {
        if (address == null) {
            return Optional.empty();
        }
        String[] tokens = address.strip().split("\\s+", 2);
        String first = tokens[0];
        return Arrays.stream(values())
                .filter(region -> region.aliases.contains(first))
                .findFirst();
    }

    /** 시·도를 판별할 수 없으면 400 VALIDATION_ERROR. */
    public static RegionCode fromAddress(String address) {
        return findByAddress(address).orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR,
                "주소에서 시·도를 확인할 수 없습니다. 주소는 시·도로 시작해야 합니다. 예: 서울 강남구 테헤란로 1"));
    }
}
