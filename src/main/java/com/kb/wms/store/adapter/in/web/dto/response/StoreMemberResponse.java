package com.kb.wms.store.adapter.in.web.dto.response;

import java.time.LocalDateTime;

import com.kb.wms.store.application.port.in.result.StoreMemberView;

/**
 * 지점 관리자 배정/조회 응답. 사용자 정보는 이름·로그인 아이디만 포함한다(비밀번호 등 민감 정보 제외).
 */
public record StoreMemberResponse(
        Long storeMemberId,
        Long storeId,
        String storeName,
        Long userId,
        String userName,
        String loginId,
        String memberRole,
        LocalDateTime assignedAt
) {

    public static StoreMemberResponse of(StoreMemberView member, String storeName) {
        return new StoreMemberResponse(
                member.storeMemberId(),
                member.storeId(),
                storeName,
                member.userId(),
                member.userName(),
                member.loginId(),
                member.memberRole(),
                member.assignedAt());
    }
}
