package com.kb.wms.auth.application.port.out;

import java.util.Collection;
import java.util.Map;

import com.kb.wms.auth.application.port.in.result.UserAffiliation;

/**
 * 회원 읽기 전용 조회 포트. 다른 도메인 테이블(창고·지점 소속)은 ID 기준 읽기 전용 조인으로만 읽는다(ADR-007).
 */
public interface UserQueryRepository {

    /** 사용자에게 배정된 창고·지점 ID. 소속이 없으면 빈 목록. */
    UserAffiliation findAffiliation(Long userId);

    /** 사용자 ID별 이름. 존재하지 않는 ID는 결과에 포함되지 않는다. 입력이 비어 있으면 빈 맵. */
    Map<Long, String> findNamesByIds(Collection<Long> userIds);
}
