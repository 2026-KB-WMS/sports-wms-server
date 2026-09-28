package com.kb.wms.store.domain.entity;

import java.time.LocalDateTime;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자와 지점의 소속·발주 역할을 연결하는 매핑.
 * 점주(STORE_OWNER)가 배정된 지점만 조회하도록 제한하는 데 사용한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreMember {

    private Long storeMemberId;
    private Long storeId;
    private Long userId;
    private String memberRole;
    private LocalDateTime assignedAt;
    private LocalDateTime createdAt;

    @Builder
    private StoreMember(Long storeMemberId, Long storeId, Long userId, String memberRole,
                        LocalDateTime assignedAt, LocalDateTime createdAt) {
        this.storeMemberId = storeMemberId;
        this.storeId = storeId;
        this.userId = userId;
        this.memberRole = memberRole;
        this.assignedAt = assignedAt;
        this.createdAt = createdAt;
    }

    public static StoreMember assign(Long storeId, Long userId, String memberRole,
                                     LocalDateTime assignedAt) {
        return StoreMember.builder()
                .storeId(storeId)
                .userId(userId)
                .memberRole(memberRole)
                .assignedAt(assignedAt)
                .build();
    }
}
