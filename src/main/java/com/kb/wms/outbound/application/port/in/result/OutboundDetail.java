package com.kb.wms.outbound.application.port.in.result;

import java.util.List;

/**
 * 출고 상세. cancelReason은 취소된 출고에만 값이 있다.
 */
public record OutboundDetail(
        OutboundView view,
        List<OutboundLineView> items,
        String cancelReason
) {
}
