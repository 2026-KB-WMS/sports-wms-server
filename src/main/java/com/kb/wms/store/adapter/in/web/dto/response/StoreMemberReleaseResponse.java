package com.kb.wms.store.adapter.in.web.dto.response;

/**
 * DELETE /api/v1/stores/managers/{storeMemberId} 응답.
 */
public record StoreMemberReleaseResponse(
        Long storeMemberId
) {
}
