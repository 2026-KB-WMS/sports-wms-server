package com.kb.wms.store.adapter.in.web.dto.response;

import java.time.LocalDateTime;

import com.kb.wms.store.domain.entity.StoreMember;

/**
 * 지점 관리자 배정/조회 응답. User 도메인이 아직 없어 userName 등 사용자 정보는 포함하지 않는다.
 */
public record StoreMemberResponse(
        Long storeMemberId,
        Long storeId,
        String storeName,
        Long userId,
        String memberRole,
        LocalDateTime assignedAt
) {

    public static StoreMemberResponse of(StoreMember member, String storeName) {
        return new StoreMemberResponse(
                member.getStoreMemberId(),
                member.getStoreId(),
                storeName,
                member.getUserId(),
                member.getMemberRole(),
                member.getAssignedAt());
    }
}
